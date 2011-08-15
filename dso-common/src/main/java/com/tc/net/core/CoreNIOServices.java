package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.exception.TCInternalError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NIOWorkarounds;
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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * The communication thread. Creates {@link Selector selector}, registers {@link SocketChannel} to the selector and does
 * other NIO operations.
 * 
 * @author mgovinda
 */

class CoreNIOServices implements TCListenerEventListener, TCConnectionEventListener {
  private static final TCLogger                logger        = TCLogging.getLogger(CoreNIOServices.class);
  private final TCWorkerCommManager            workerCommMgr;
  private final String                         commThreadName;
  private final SocketParams                   socketParams;
  private final CommThread                     readerComm;
  private final CommThread                     writerComm;
  private final SetOnceFlag                    stopRequested = new SetOnceFlag();

  // maintains weight of all L1 Connections which is handled by this WorkerComm
  private final HashMap<TCConnection, Integer> managedConnectionsMap;
  private int                                  clientWeights;
  private final List                           listeners     = new ArrayList();
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

  public void cleanupChannel(SocketChannel channel, Runnable callback) {
    readerComm.cleanupChannel(channel, callback);
    writerComm.cleanupChannel(channel, callback);
  }

  public void detach(final SocketChannel channel) {
    readerComm.unregister(channel);
    writerComm.unregister(channel);
  }

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
      TCListener listener = (TCListener) listeners.get(i);
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

  public int getWeight() {
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
  public void addWeight(final TCConnectionImpl connection, final int addWeightBy, final SocketChannel channel) {

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
    synchronized (managedConnectionsMap) {
      final CoreNIOServices workerComm = workerCommMgr.getNextWorkerComm();
      connection.setCommWorker(workerComm);
      workerComm.addConnection(connection, addWeightBy);
      workerComm.requestReadWriteInterest(connection, channel);
    }
  }

  private void addConnection(TCConnectionImpl connection, int initialWeight) {
    synchronized (managedConnectionsMap) {
      Assert.eval(!managedConnectionsMap.containsKey(connection));
      managedConnectionsMap.put(connection, initialWeight);
      this.clientWeights += initialWeight;
      connection.addListener(this);
    }
  }

  public void closeEvent(TCConnectionEvent event) {
    synchronized (managedConnectionsMap) {
      Assert.eval(managedConnectionsMap.containsKey(event.getSource()));
      int closedCientWeight = managedConnectionsMap.get(event.getSource());
      this.clientWeights -= closedCientWeight;
      managedConnectionsMap.remove(event.getSource());
      event.getSource().removeListener(this);
    }
  }

  public void connectEvent(TCConnectionEvent event) {
    //
  }

  public void endOfFileEvent(TCConnectionEvent event) {
    //
  }

  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    //
  }

  @Override
  public synchronized String toString() {
    return "[" + this.commThreadName + ", FD, wt:" + getWeight() + "]";
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
    private final Selector         selector;
    private final LinkedQueue      selectorTasks;
    private final String           name;
    private final SynchronizedLong bytesRead    = new SynchronizedLong(0);
    private final SynchronizedLong bytesWritten = new SynchronizedLong(0);
    private final COMM_THREAD_MODE mode;

    public CommThread(final COMM_THREAD_MODE mode) {
      name = commThreadName + (mode == COMM_THREAD_MODE.NIO_READER ? "_R" : "_W");
      setDaemon(true);
      setName(name);

      this.selector = createSelector();
      this.selectorTasks = new LinkedQueue();
      this.mode = mode;
    }

    private boolean isReader() {
      return (this.mode == COMM_THREAD_MODE.NIO_READER);
    }

    @Override
    public void run() {
      try {
        selectLoop();
      } catch (Throwable t) {
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
          } catch (NullPointerException npe) {
            if (i < tries && NIOWorkarounds.selectorOpenRace(npe)) {
              System.err
                  .println("Attempting to work around sun bug 6427854 (attempt " + (i + 1) + " of " + tries + ")");
              try {
                Thread.sleep(new Random().nextInt(20) + 5);
              } catch (InterruptedException ie) {
                interrupted = true;
              }
              continue;
            }
            throw npe;
          }
        }
      } finally {
        Util.selfInterruptIfNeeded(interrupted);
      }

      return selector1;
    }

    void addSelectorTask(final Runnable task) {
      boolean isInterrupted = false;

      try {
        while (true) {
          try {
            this.selectorTasks.put(task);
            break;
          } catch (InterruptedException e) {
            logger.warn(e);
            isInterrupted = true;
          }
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
        logger.error(e);
      } finally {
        try {
          callback.run();
        } catch (Exception e) {
          logger.error(e);
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

    private void dispose(Selector localSelector, LinkedQueue localSelectorTasks) {
      Assert.eval(Thread.currentThread() == this);

      if (localSelector != null) {

        for (Object element : localSelector.keys()) {
          try {
            SelectionKey key = (SelectionKey) element;
            cleanupChannel(key.channel(), null);
          }

          catch (Exception e) {
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
      LinkedQueue localSelectorTasks = this.selectorTasks;

      while (true) {
        final int numKeys;
        try {
          numKeys = localSelector.select();
        } catch (IOException ioe) {
          if (NIOWorkarounds.linuxSelectWorkaround(ioe)) {
            logger.warn("working around Sun bug 4504001");
            continue;
          }
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
          Runnable task = null;
          while (true) {
            try {
              task = (Runnable) localSelectorTasks.poll(0);
              break;
            } catch (InterruptedException ie) {
              logger.error("Error getting task from task queue", ie);
              isInterrupted = true;
            }
          }

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

        final Set selectedKeys = localSelector.selectedKeys();
        if ((0 == numKeys) && (0 == selectedKeys.size())) {
          continue;
        }

        for (Iterator iter = selectedKeys.iterator(); iter.hasNext();) {
          SelectionKey key = (SelectionKey) iter.next();
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
              ScatteringByteChannel channel = (ScatteringByteChannel) key.channel();
              do {
                read = reader.doRead(channel);
                this.bytesRead.add(read);
              } while ((read != 0) && key.isReadable());
            }

            if (key.isValid() && !isReader() && key.isWritable()) {
              int written = ((TCChannelWriter) key.attachment()).doWrite((GatheringByteChannel) key.channel());
              this.bytesWritten.add(written);
            }

          } catch (CancelledKeyException cke) {
            logger.info("selection key cancelled key@" + key.hashCode());
          }
        } // for
      } // while (true)
    }

    private void doAccept(final SelectionKey key) {
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
      SocketChannel sc = (SocketChannel) key.channel();
      TCConnectionImpl conn = (TCConnectionImpl) key.attachment();

      try {
        if (sc.finishConnect()) {
          sc.register(selector, SelectionKey.OP_READ, conn);
          conn.finishConnect();
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
      return this.bytesRead.get();
    }

    public long getTotalBytesWritten() {
      return this.bytesWritten.get();
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
          logger.debug(request);
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