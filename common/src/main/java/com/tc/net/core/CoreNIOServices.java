/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.exception.TCInternalError;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.util.Assert;
import com.tc.util.Util;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.runtime.Os;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The communication thread. Creates {@link Selector selector}, registers {@link SocketChannel} to the selector and does
 * other NIO operations.
 *
 * @author mgovinda
 */

class CoreNIOServices implements TCListenerEventListener, TCConnectionEventListener {
  private static final Logger logger = LoggerFactory.getLogger(CoreNIOServices.class);
  private final TCWorkerCommManager            workerCommMgr;
  private final String                         commThreadName;
  private final SocketParams                   socketParams;
  private final CommThread                     readerComm;
  private final CommThread                     writerComm;
  private final SetOnceFlag                    stopRequested = new SetOnceFlag();

  // maintains weight of all L1 Connections which is handled by this WorkerComm
  private final HashMap<TCConnection, Integer> managedConnectionsMap;
  private int                                  clientWeights;
  private boolean                              isSelectedForWeighting;
  private final List<TCListener>               listeners     = new ArrayList<TCListener>();
  private String                               listenerString;

  private static enum COMM_THREAD_MODE {
    NIO_READER, NIO_WRITER
  }

  public CoreNIOServices(String commThreadName, TCWorkerCommManager workerCommManager, SocketParams socketParams) {
    this.commThreadName = commThreadName;
    this.workerCommMgr = workerCommManager;
    this.socketParams = socketParams;
    this.managedConnectionsMap = new HashMap<TCConnection, Integer>();
    this.readerComm = new CommThread(COMM_THREAD_MODE.NIO_READER);
    this.writerComm = new CommThread(COMM_THREAD_MODE.NIO_WRITER);
  }

  public void start() {
    readerComm.start();
    writerComm.start();
  }

  public void requestStop() {
    if (stopRequested.attemptSet()) {
      readerComm.requestStop();
      writerComm.requestStop();
    }
  }

  public void cleanupChannel(final SocketChannel channel, final Runnable callback) {
//  shutdown the writer side first, the the read side.  Doing this because
//  cleanup can race with handshake if the writer side is shutdown after or in parallel
    writerComm.cleanupChannel(channel, new Runnable() {
      @Override
      public void run() {
        readerComm.cleanupChannel(channel, callback);
      }
    });
  }

  @Override
  public void closeEvent(TCListenerEvent event) {
    listenerRemoved(event.getSource());
  }

  public void registerListener(TCListenerImpl lsnr, ServerSocketChannel ssc) {
    requestAcceptInterest(lsnr, ssc);
    listenerAdded(lsnr);
    lsnr.addEventListener(this);
  }

  // listener was with readerComm only
  public void stopListener(ServerSocketChannel ssc, Runnable callback) {
    readerComm.stopListener(ssc, callback);
  }

  private synchronized void listenerRemoved(TCListener listener) {
    boolean removed = listeners.remove(listener);
    Assert.eval(removed);
    updateListenerString();
    readerComm.updateThreadName();
    writerComm.updateThreadName();
  }

  private synchronized void listenerAdded(TCListener listener) {
    listeners.add(listener);
    updateListenerString();
    readerComm.updateThreadName();
    writerComm.updateThreadName();
  }

  private void updateListenerString() {
    if (listeners.isEmpty()) {
      listenerString = "";
    }
    StringBuffer buf = new StringBuffer();
    buf.append(" (listen ");
    for (int i = 0, n = listeners.size(); i < n; i++) {
      TCListener listener = listeners.get(i);
      buf.append(listener.getBindAddress().getHostAddress());
      buf.append(':');
      buf.append(listener.getBindPort());
      if (i < (n - 1)) {
        buf.append(',');
      }
    }
    buf.append(')');
    listenerString = buf.toString();
  }

  private synchronized String getListenerString() {
    return this.listenerString;
  }

  public long getTotalBytesRead() {
    return readerComm.getTotalBytesRead() + writerComm.getTotalBytesRead();
  }

  public long getTotalBytesWritten() {
    return readerComm.getTotalBytesWritten() + writerComm.getTotalBytesWritten();
  }
  
  public static boolean hasPendingReads() {
    Thread t = Thread.currentThread();
    if (t instanceof CommThread) {
      CommThread ct = (CommThread)t;
      if (ct.isReader()) {
        try {
          return ct.selector.selectedKeys().stream().anyMatch(key->{
            try {
              return key.isValid() && key.isReadable();
            } catch (CancelledKeyException ck) {
              return false;
            }
          });
        } catch (ClosedSelectorException closed) {
          return false;
        }
      }
    }
    return false;
  }
  
  public boolean compareWeights(CoreNIOServices incoming) {
    boolean retVal = false;
// if incoming is passed in, the current search is the one that set the flag
// so it is ok to assert the below is true
    Assert.assertTrue(incoming == null || incoming.isSelectedForWeighting);
    
    synchronized (managedConnectionsMap) {
      if (!isSelectedForWeighting) {
        if (incoming == null || incoming.clientWeights > this.clientWeights) {
          this.isSelectedForWeighting = true;
          retVal = true;
        }
      }
    }
//  if trading, unset the previous selected
    if (retVal && incoming != null) {
      incoming.deselectForWeighting();
    }

    return retVal;
  }
  
  private void deselectForWeighting() {
    synchronized (managedConnectionsMap) {
      isSelectedForWeighting = false;
    }
  }

  int getWeight() {
    synchronized (managedConnectionsMap) {
      return this.clientWeights;
    }
  }

  protected CommThread getReaderComm() {
    return this.readerComm;
  }

  protected CommThread getWriterComm() {
    return this.writerComm;
  }

  /**
   * Change thread ownership of a connection or upgrade weight.
   *
   * @param connection : connection which has to be transfered from the main selector thread to a new worker comm thread
   *        that has the least weight. If the connection is already managed by a comm thread, then just update
   *        connection's weight.
   * @param addWeightBy : upgrade weight of connection
   * @param channel : SocketChannel for the passed in connection
   */
  public void addWeight(TCConnectionImpl connection, int addWeightBy, SocketChannel channel) {

    synchronized (managedConnectionsMap) {
      // this connection is already handled by a WorkerComm
      if (this.managedConnectionsMap.containsKey(connection)) {
        this.clientWeights += addWeightBy;
        this.managedConnectionsMap.put(connection, this.managedConnectionsMap.get(connection) + addWeightBy);
        return;
      }
    }

    // MainComm Thread
    if (workerCommMgr == null) { return; }

    readerComm.unregister(channel);
    final CoreNIOServices workerComm = workerCommMgr.getNextWorkerComm();
    connection.setCommWorker(workerComm);
    workerComm.addConnection(connection, addWeightBy);
    workerComm.requestReadWriteInterest(connection, channel);
  }

  private void addConnection(TCConnectionImpl connection, int initialWeight) {
    synchronized (managedConnectionsMap) {
      Assert.eval(!managedConnectionsMap.containsKey(connection));
      managedConnectionsMap.put(connection, initialWeight);
      this.clientWeights += initialWeight;
      this.isSelectedForWeighting = false;
      connection.addListener(this);
    }
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    synchronized (managedConnectionsMap) {
      Assert.eval(managedConnectionsMap.containsKey(event.getSource()));
      int closedCientWeight = managedConnectionsMap.get(event.getSource());
      this.clientWeights -= closedCientWeight;
      managedConnectionsMap.remove(event.getSource());
      event.getSource().removeListener(this);
    }
  }

  @Override
  public void connectEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }

  @Override
  public String toString() {
    synchronized (this.managedConnectionsMap) {
      return "[" + this.commThreadName + ", FD, wt:" + this.clientWeights + "]";
    }
  }
  
  public String getName() {
    return this.commThreadName;
  }
  
  public Map<String, ?> getState() {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("name", this.commThreadName);
    state.put("weights", this.clientWeights);
    state.put("writer", this.writerComm.getCommState());
    state.put("reader", this.readerComm.getCommState());
    return state;
  }

  void requestConnectInterest(TCConnectionImpl conn, SocketChannel sc) {
    readerComm.requestConnectInterest(conn, sc);
  }

  private void requestAcceptInterest(TCListenerImpl lsnr, ServerSocketChannel ssc) {
    readerComm.requestAcceptInterest(lsnr, ssc);
  }

  void requestReadInterest(TCChannelReader reader, ScatteringByteChannel channel) {
    readerComm.requestReadInterest(reader, channel);
  }

  void removeReadInterest(TCConnectionImpl conn, SelectableChannel channel) {
    readerComm.removeReadInterest(conn, channel);
  }

  void requestWriteInterest(TCChannelWriter writer, GatheringByteChannel channel) {
    writerComm.requestWriteInterest(writer, channel);
  }

  void removeWriteInterest(TCConnectionImpl conn, SelectableChannel channel) {
    writerComm.removeWriteInterest(conn, channel);
  }

  private void requestReadWriteInterest(TCConnectionImpl conn, SocketChannel sc) {
    readerComm.requestReadInterest(conn, sc);
    writerComm.requestWriteInterest(conn, sc);
  }

  protected class CommThread extends Thread {
    private final Selector                      selector;
    private final Queue<Runnable> selectorTasks;
    private final String                        name;
    private long                    bytesMoved    = 0;
    private final COMM_THREAD_MODE              mode;

    public CommThread(COMM_THREAD_MODE mode) {
      name = commThreadName + (mode == COMM_THREAD_MODE.NIO_READER ? "_R" : "_W");
      setDaemon(true);
      setName(name);

      this.selector = createSelector();
      this.selectorTasks = new ConcurrentLinkedQueue<Runnable>();
      this.mode = mode;
    }
    
    private Map<String, ?> getCommState() {
      Map<String, Object> state = new LinkedHashMap<>();
      state.put("name", name);
      state.put("mode", mode);
      state.put("bytesMoved", bytesMoved);
      state.put("selectorBacklog", selectorTasks.size());
      return state;
    }

    private boolean isReader() {
      return (this.mode == COMM_THREAD_MODE.NIO_READER);
    }

    @Override
    public void run() {
      try {
        selectLoop();
      } catch (Throwable t) {
        // if something goes wrong on selector level, we cannot recover
        logger.error("Unhandled exception from selectLoop", t);
        throw new RuntimeException(t);
      } finally {
        dispose(selector, selectorTasks);
      }
    }

    public void requestStop() {
      try {
        this.selector.wakeup();
      } catch (Exception e) {
        logger.error("Exception trying to stop " + getName() + ": ", e);
      }
    }

    private void updateThreadName() {
      setName(name + getListenerString());
    }

    @SuppressWarnings("resource")
    private Selector createSelector() {
      Selector selector1 = null;

      final int tries = 3;

      boolean interrupted = false;
      try {
        for (int i = 0; i < tries; i++) {
          try {
            selector1 = Selector.open();
            return selector1;
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        }
      } finally {
        Util.selfInterruptIfNeeded(interrupted);
      }

      return selector1;
    }

    void addSelectorTask(Runnable task) {
      boolean isInterrupted = false;

      try {
        while (true) {
          while (!this.selectorTasks.offer(task)) {
            try {
              TimeUnit.SECONDS.sleep(5);  // this should actually never happen since the queue is unbounded.  
              logger.warn("unable to add selector task");
            } catch (InterruptedException ie) {
              isInterrupted = true;
            }
          }
          break;
        }
      } finally {
        this.selector.wakeup();
        Util.selfInterruptIfNeeded(isInterrupted);
      }
    }

    void unregister(final SelectableChannel channel) {
      if (Thread.currentThread() != this) {
        final CountDownLatch latch = new CountDownLatch(1);
        this.addSelectorTask(new Runnable() {
          @Override
          public void run() {
            CommThread.this.unregister(channel);
            latch.countDown();
          }
        });
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      } else {
        SelectionKey key = null;
        key = channel.keyFor(this.selector);
        if (key != null) {
          key.cancel();
          key.attach(null);
        }
      }
    }

    void stopListener(final ServerSocketChannel ssc, final Runnable callback) {
      if (Thread.currentThread() != this) {
        Runnable task = new Runnable() {
          @Override
          public void run() {
            CommThread.this.stopListener(ssc, callback);
          }
        };
        addSelectorTask(task);
        return;
      }

      try {
        cleanupChannel(ssc, null);
      } catch (Exception e) {
        logger.error("Exception: ", e);
      } finally {
        try {
          callback.run();
        } catch (Exception e) {
          logger.error("Exception: ", e);
        }
      }
    }

    void cleanupChannel(final Channel ch, final Runnable callback) {

      if (null == ch) {
        // not expected
        logger.warn("null channel passed to cleanupChannel()", new Throwable());
        return;
      }

      if (Thread.currentThread() != this) {
        if (logger.isDebugEnabled()) {
          logger.debug("queue'ing channel close operation");
        }

        addSelectorTask(new Runnable() {
          @Override
          public void run() {
            CommThread.this.cleanupChannel(ch, callback);
          }
        });
        return;
      }

      try {
        if (ch instanceof SelectableChannel) {
          SelectableChannel sc = (SelectableChannel) ch;

          try {
            SelectionKey sk = sc.keyFor(selector);
            if (sk != null) {
              sk.attach(null);
              sk.cancel();
            }
          } catch (Exception e) {
            logger.warn("Exception trying to clear selection key", e);
          }
        }

        if (ch instanceof SocketChannel) {
          SocketChannel sc = (SocketChannel) ch;

          @SuppressWarnings("resource")
          Socket s = sc.socket();

          if (null != s) {
            synchronized (s) {

              if (s.isConnected()) {
                try {
                  if (!s.isOutputShutdown()) {
                    s.shutdownOutput();
                  }
                } catch (Exception e) {
                  logger.warn("Exception trying to shutdown socket output: " + e.getMessage());
                }

                try {
                  if (!s.isClosed()) {
                    s.close();
                  }
                } catch (Exception e) {
                  logger.warn("Exception trying to close() socket: " + e.getMessage());
                }
              }
            }
          }
        } else if (ch instanceof ServerSocketChannel) {
          ServerSocketChannel ssc = (ServerSocketChannel) ch;

          try {
            ssc.close();
          } catch (Exception e) {
            logger.warn("Exception trying to close() server socket" + e.getMessage());
          }
        }

        try {
          ch.close();
        } catch (Exception e) {
          logger.warn("Exception trying to close channel", e);
        }
      } catch (Exception e) {
        // this is just a catch all to make sure that no exceptions will be thrown by this method, please do not remove
        logger.error("Unhandled exception in cleanupChannel()", e);
      } finally {
        try {
          if (callback != null) {
            callback.run();
          }
        } catch (Throwable t) {
          logger.error("Unhandled exception in cleanupChannel callback.", t);
        }
      }
    }

    private void dispose(Selector localSelector, Queue<Runnable> localSelectorTasks) {
      Assert.eval(Thread.currentThread() == this);

      if (localSelector != null) {

        for (SelectionKey key : localSelector.keys()) {
          try {
            cleanupChannel(key.channel(), null);
          } catch (Exception e) {
            logger.warn("Exception trying to close channel", e);
          }
        }

        try {
          localSelector.close();
        } catch (Exception e) {
          if ((Os.isMac()) && (Os.isUnix()) && (e.getMessage().equals("Bad file descriptor"))) {
            // I can't find a specific bug about this, but I also can't seem to prevent the exception on the Mac.
            // So just logging this as warning.
            logger.warn("Exception trying to close selector: " + e.getMessage());
          } else {
            logger.error("Exception trying to close selector", e);
          }
        }
      }
    }

    private void selectLoop() throws IOException {
      Assert.eval(Thread.currentThread() == this);

      Selector localSelector = this.selector;
      Queue<Runnable> localSelectorTasks = this.selectorTasks;

      while (true) {
        final int numKeys;
        try {
          numKeys = localSelector.select();
        } catch (IOException ioe) {
          throw ioe;
        } catch (CancelledKeyException cke) {
          logger.warn("Cencelled Key " + cke);
          continue;
        }

        if (isStopRequested()) {
          if (logger.isDebugEnabled()) {
            logger.debug("Select loop terminating");
          }
          return;
        }

        boolean isInterrupted = false;
        // run any pending selector tasks
        while (true) {
          Runnable task = localSelectorTasks.poll();

          if (null == task) {
            break;
          }

          try {
            task.run();
          } catch (Exception e) {
            logger.error("error running selector task", e);
          }
        }
        Util.selfInterruptIfNeeded(isInterrupted);

        final Set<SelectionKey> selectedKeys = localSelector.selectedKeys();
        if ((0 == numKeys) && (0 == selectedKeys.size())) {
          continue;
        }

        for (Iterator<SelectionKey> iter = selectedKeys.iterator(); iter.hasNext();) {
          SelectionKey key = iter.next();
          iter.remove();

          if (null == key) {
            logger.error("Selection key is null");
            continue;
          }

          try {

            if (key.isAcceptable()) {
              doAccept(key);
              continue;
            }

            if (key.isConnectable()) {
              doConnect(key);
              continue;
            }

            if (isReader() && key.isValid() && key.isReadable()) {
              int read;
              TCChannelReader reader = (TCChannelReader) key.attachment();
              do {
                read = reader.doRead();
                bytesMoved += read;
              } while ((read != 0) && key.isReadable());
            }

            if (key.isValid() && !isReader() && key.isWritable()) {
              int written = ((TCChannelWriter) key.attachment()).doWrite();
                bytesMoved += written;
            }

            TCConnection conn = (TCConnection) key.attachment();
            if (conn != null && conn.isClosePending()) {
              conn.asynchClose();
            }

          } catch (CancelledKeyException cke) {
            logger.debug("selection key cancelled key@" + key.hashCode());
          } catch (Exception e) { // DEV-9369. Do not reconnect on fatal errors.
            logger.info("Unhandled exception occured on connection layer", e);
            Object attachment = key.attachment();
            if (attachment instanceof TCConnectionImpl) {
              TCConnectionImpl conn = (TCConnectionImpl) attachment;
              // TCConnectionManager will take care of closing and cleaning up resources
              // key may not have an attachment yet.
              if (conn != null) {
                conn.fireErrorEvent(new RuntimeException(e), null);
              }
            } else if (attachment instanceof TCListenerImpl) {
              TCListenerImpl lsnr = (TCListenerImpl) key.attachment();
              // just log
            }

          }
        } // for
      } // while (true)
    }

    @SuppressWarnings("resource")
    private void doAccept(SelectionKey key) {
      SocketChannel sc = null;

      final TCListenerImpl lsnr = (TCListenerImpl) key.attachment();

      try {
        final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        sc = ssc.accept();
        if (sc == null) {
          // non blocking channel accept can return null
          logger.warn("New connection accept didn't go through for " + ssc.socket());
          return;
        }
        sc.configureBlocking(false);
        final TCConnectionImpl conn = lsnr.createConnection(sc, CoreNIOServices.this, socketParams);
        requestReadInterest(conn, sc);
      } catch (IOException ioe) {
        if (logger.isInfoEnabled()) {
          logger.info("IO Exception accepting new connection", ioe);
        }

        cleanupChannel(sc, null);
      }
    }

    private void doConnect(SelectionKey key) {
      @SuppressWarnings("resource")
      SocketChannel sc = (SocketChannel) key.channel();
      
      TCConnectionImpl conn = (TCConnectionImpl) key.attachment();

      try {
        if (sc.finishConnect()) {
          conn.finishConnect();
          sc.register(selector, SelectionKey.OP_READ, conn);
        } else {
          String errMsg = "finishConnect() returned false, but no exception thrown";

          if (logger.isInfoEnabled()) {
            logger.info(errMsg);
          }

          conn.fireErrorEvent(new Exception(errMsg), null);
        }
      } catch (IOException ioe) {
        if (logger.isInfoEnabled()) {
          logger.info("IOException attempting to finish socket connection", ioe);
        }

        conn.fireErrorEvent(ioe, null);
      }
    }

    public long getTotalBytesRead() {
      return this.mode == COMM_THREAD_MODE.NIO_READER ? this.bytesMoved : 0;
    }

    public long getTotalBytesWritten() {
      return this.mode == COMM_THREAD_MODE.NIO_WRITER ? this.bytesMoved : 0;
    }

    private void handleRequest(final InterestRequest req) {
      // ignore the request if we are stopped/stopping
      if (isStopRequested()) { return; }

      if (Thread.currentThread() == this) {
        modifyInterest(req);
      } else {
        final CommThread commTh = req.getCommNIOServiceThread();
        Assert.assertNotNull(commTh);
        commTh.addSelectorTask(new Runnable() {
          @Override
          public void run() {
            commTh.handleRequest(req);
          }
        });
      }
    }

    private boolean isStopRequested() {
      return stopRequested.isSet();
    }

    private void modifyInterest(InterestRequest request) {
      Assert.eval(Thread.currentThread() == this);

      Selector localSelector = null;
      localSelector = selector;

      try {
        final int existingOps;

        SelectionKey key = request.channel.keyFor(localSelector);
        if (key != null) {
          if (!key.isValid()) {
            logger.warn("Skipping modifyInterest - " + Constants.interestOpsToString(request.interestOps) + " on "
                        + request.attachment);
            return;
          }
          existingOps = key.interestOps();
        } else {
          existingOps = 0;
        }

        if (logger.isDebugEnabled()) {
          logger.debug("{}", request);
        }

        if (request.add) {
          request.channel.register(localSelector, existingOps | request.interestOps, request.attachment);
        } else if (request.set) {
          request.channel.register(localSelector, request.interestOps, request.attachment);
        } else if (request.remove) {
          request.channel.register(localSelector, existingOps ^ request.interestOps, request.attachment);
        } else {
          throw new TCInternalError();
        }
      } catch (ClosedChannelException cce) {
        logger.warn("Exception trying to process interest request: " + cce);
      } catch (CancelledKeyException cke) {
        logger.warn("Exception trying to process interest request: " + cke);
      }
    }

    void requestConnectInterest(TCConnectionImpl conn, SocketChannel sc) {
      handleRequest(InterestRequest.createSetInterestRequest(sc, conn, SelectionKey.OP_CONNECT, this));
    }

    void requestReadInterest(TCChannelReader reader, ScatteringByteChannel channel) {
      Assert.eval(isReader());
      handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, reader, SelectionKey.OP_READ,
                                                             this));
    }

    void requestWriteInterest(TCChannelWriter writer, GatheringByteChannel channel) {
      Assert.eval(!isReader());
      handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, writer,
                                                             SelectionKey.OP_WRITE, this));
    }

    private void requestAcceptInterest(TCListenerImpl lsnr, ServerSocketChannel ssc) {
      Assert.eval(isReader());
      handleRequest(InterestRequest.createSetInterestRequest(ssc, lsnr, SelectionKey.OP_ACCEPT, this));
    }

    void removeWriteInterest(TCConnectionImpl conn, SelectableChannel channel) {
      Assert.eval(!isReader());
      handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_WRITE, this));
    }

    void removeReadInterest(TCConnectionImpl conn, SelectableChannel channel) {
      Assert.eval(isReader());
      handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_READ, this));
    }
  }

  private static class InterestRequest {
    final SelectableChannel channel;
    final Object            attachment;
    final boolean           set;
    final boolean           add;
    final boolean           remove;
    final int               interestOps;
    final CommThread        commNIOServiceThread;

    static InterestRequest createAddInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    CommThread nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, false, true, false, nioServiceThread);
    }

    static InterestRequest createSetInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    CommThread nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, true, false, false, nioServiceThread);
    }

    static InterestRequest createRemoveInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                       CommThread nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, false, false, true, nioServiceThread);
    }

    private InterestRequest(SelectableChannel channel, Object attachment, int interestOps, boolean set, boolean add,
                            boolean remove, CommThread nioServiceThread) {
      Assert.eval(remove ^ set ^ add);
      Assert.eval(channel != null);

      this.channel = channel;
      this.attachment = attachment;
      this.set = set;
      this.add = add;
      this.remove = remove;
      this.interestOps = interestOps;
      this.commNIOServiceThread = nioServiceThread;
    }

    public CommThread getCommNIOServiceThread() {
      return commNIOServiceThread;
    }

    @Override
    public String toString() {
      StringBuffer buf = new StringBuffer();

      buf.append("Interest modify request: ").append(channel.toString()).append("\n");
      buf.append("Ops: ").append(Constants.interestOpsToString(interestOps)).append("\n");
      buf.append("Set: ").append(set).append(", Remove: ").append(remove).append(", Add: ").append(add).append("\n");
      buf.append("Attachment: ");

      if (attachment != null) {
        buf.append(attachment.toString());
      } else {
        buf.append("null");
      }

      buf.append("\n");

      return buf.toString();
    }
  }
}
