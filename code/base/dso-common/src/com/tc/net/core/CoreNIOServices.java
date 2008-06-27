package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.exception.TCInternalError;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NIOWorkarounds;
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

class CoreNIOServices extends Thread implements TCListenerEventListener {
  private static final TCLogger     logger        = TCLogging.getLogger(CoreNIOServices.class);

  private final Selector            selector;
  private final LinkedQueue         selectorTasks;
  private final String              baseThreadName;
  private final TCWorkerCommManager workerCommMgr;
  private final List                listeners     = new ArrayList();
  private final SocketParams        socketParams;
  private final SynchronizedLong    bytesRead     = new SynchronizedLong(0);
  private final SetOnceFlag         stopRequested = new SetOnceFlag();

  public CoreNIOServices(String commThreadName, TCWorkerCommManager workerCommManager, SocketParams socketParams) {
    setDaemon(true);
    setName(commThreadName);

    this.selector = createSelector();
    this.selectorTasks = new LinkedQueue();

    this.socketParams = socketParams;
    this.baseThreadName = commThreadName;
    this.workerCommMgr = workerCommManager;
  }

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
    if (stopRequested.attemptSet()) {
      try {
        this.selector.wakeup();
      } catch (Exception e) {
        logger.error("Exception trying to stop " + getName() + ": ", e);
      }
    }
  }

  private String makeListenerString() {
    if (listeners.isEmpty()) { return ""; }

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

    return buf.toString();
  }

  private synchronized void listenerRemoved(TCListener listener) {
    boolean removed = listeners.remove(listener);
    Assert.eval(removed);
    updateThreadName();
  }

  private synchronized void listenerAdded(TCListener listener) {
    listeners.add(listener);
    updateThreadName();
  }

  private void updateThreadName() {
    StringBuffer buf = new StringBuffer(baseThreadName);
    buf.append(makeListenerString());
    setName(buf.toString());
  }

  private Selector createSelector() {
    Selector selector1 = null;

    final int tries = 3;

    for (int i = 0; i < tries; i++) {
      try {
        selector1 = Selector.open();
        return selector1;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      } catch (NullPointerException npe) {
        if (i < tries && NIOWorkarounds.selectorOpenRace(npe)) {
          System.err.println("Attempting to work around sun bug 6427854 (attempt " + (i + 1) + " of " + tries + ")");
          try {
            Thread.sleep(new Random().nextInt(20) + 5);
          } catch (InterruptedException ie) {
            //
          }
          continue;
        }
        throw npe;
      }
    }

    return selector1;
  }

  void addSelectorTask(final Runnable task) {
    Assert.eval(Thread.currentThread() != this);
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
    }

    Util.selfInterruptIfNeeded(isInterrupted);

  }

  void unregister(SelectableChannel channel) {
    SelectionKey key = null;
    Assert.eval(Thread.currentThread() == this);
    key = channel.keyFor(this.selector);

    if (key != null) {
      key.cancel();
      key.attach(null);
    }
  }

  void stopListener(final ServerSocketChannel ssc, final Runnable callback) {
    if (Thread.currentThread() != this) {
      Runnable task = new Runnable() {
        public void run() {
          CoreNIOServices.this.stopListener(ssc, callback);
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
          CoreNIOServices.this.cleanupChannel(ch, callback);
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

      for (Iterator keys = localSelector.keys().iterator(); keys.hasNext();) {
        try {
          SelectionKey key = (SelectionKey) keys.next();
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

          if (key.isReadable()) {
            int read = ((TCJDK14ChannelReader) key.attachment()).doRead((ScatteringByteChannel) key.channel());
            this.bytesRead.add(read);
          }

          if (key.isValid() && key.isWritable()) {
            ((TCJDK14ChannelWriter) key.attachment()).doWrite((GatheringByteChannel) key.channel());
          }

        } catch (CancelledKeyException cke) {
          logger.warn(cke.getClass().getName() + " occured");
        }
      } // for
    } // while (true)
  }

  private void doAccept(final SelectionKey key) {
    SocketChannel sc = null;

    final TCListenerJDK14 lsnr = (TCListenerJDK14) key.attachment();

    try {
      final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      sc = ssc.accept();
      sc.configureBlocking(false);

      if (workerCommMgr == null) {
        // Single threaded server model
        final TCConnectionJDK14 conn = lsnr.createConnection(sc, this, socketParams);
        sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, conn);

        return;
      }
      
      // Multi threaded server model
      final CoreNIOServices workerCommThread = workerCommMgr.getNextWorkerComm();
      final TCConnectionJDK14 conn = lsnr.createConnection(sc, workerCommThread, socketParams);
      
      workerCommThread.requestReadWriteInterest(conn, sc);
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("IO Exception accepting new connection", ioe);
      }

      cleanupChannel(sc, null);
      return;
    }
  }

  private void doConnect(SelectionKey key) {
    SocketChannel sc = (SocketChannel) key.channel();
    TCConnectionJDK14 conn = (TCConnectionJDK14) key.attachment();

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

  private void handleRequest(final InterestRequest req) {
    // ignore the request if we are stopped/stopping
    if (isStopRequested()) { return; }

    if (Thread.currentThread() == this) {
      modifyInterest(req);
    } else {
      final CoreNIOServices commTh = req.getCommNIOServiceThread();
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

  void requestConnectInterest(TCConnectionJDK14 conn, SocketChannel sc) {
    handleRequest(InterestRequest.createSetInterestRequest(sc, conn, SelectionKey.OP_CONNECT, this));
  }

  void requestReadInterest(TCJDK14ChannelReader reader, ScatteringByteChannel channel) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, reader, SelectionKey.OP_READ,
                                                           this));
  }

  void requestWriteInterest(TCJDK14ChannelWriter writer, GatheringByteChannel channel) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, writer, SelectionKey.OP_WRITE,
                                                           this));
  }

  private void requestAcceptInterest(TCListenerJDK14 lsnr, ServerSocketChannel ssc) {
    handleRequest(InterestRequest.createSetInterestRequest(ssc, lsnr, SelectionKey.OP_ACCEPT, this));
  }

  void removeWriteInterest(TCConnectionJDK14 conn, SelectableChannel channel) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_WRITE, this));
  }

  void removeReadInterest(TCConnectionJDK14 conn, SelectableChannel channel) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_READ, this));
  }

  private void requestReadWriteInterest(TCConnectionJDK14 conn, SocketChannel sc) {
    handleRequest(InterestRequest
        .createAddInterestRequest(sc, conn, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this));
  }

  private static class InterestRequest {
    final SelectableChannel channel;
    final Object            attachment;
    final boolean           set;
    final boolean           add;
    final boolean           remove;
    final int               interestOps;
    final CoreNIOServices   commNIOServiceThread;

    static InterestRequest createAddInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    CoreNIOServices nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, false, true, false, nioServiceThread);
    }

    static InterestRequest createSetInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                    CoreNIOServices nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, true, false, false, nioServiceThread);
    }

    static InterestRequest createRemoveInterestRequest(SelectableChannel channel, Object attachment, int interestOps,
                                                       CoreNIOServices nioServiceThread) {
      return new InterestRequest(channel, attachment, interestOps, false, false, true, nioServiceThread);
    }

    private InterestRequest(SelectableChannel channel, Object attachment, int interestOps, boolean set, boolean add,
                            boolean remove, CoreNIOServices nioServiceThread) {
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

    public CoreNIOServices getCommNIOServiceThread() {
      return commNIOServiceThread;
    }

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

  public void closeEvent(TCListenerEvent event) {
    listenerRemoved(event.getSource());
  }

  public void registerListener(TCListenerJDK14 lsnr, ServerSocketChannel ssc) {
    requestAcceptInterest(lsnr, ssc);
    listenerAdded(lsnr);
    lsnr.addEventListener(this);
  }

}
