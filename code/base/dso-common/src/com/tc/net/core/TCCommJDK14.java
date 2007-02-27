/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCInternalError;
import com.tc.net.NIOWorkarounds;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.util.Assert;
import com.tc.util.Util;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 * JDK 1.4 (NIO) version of TCComm. Uses a single internal thread and a selector to manage channels associated with
 * <code>TCConnection</code>'s
 * 
 * @author teck
 */
class TCCommJDK14 extends AbstractTCComm {

  TCCommJDK14() {
    // nada
  }

  protected void startImpl() {
    this.selector = null;

    final int tries = 3;

    for (int i = 0; i < tries; i++) {
      try {
        this.selector = Selector.open();
        break;
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

    if (this.selector == null) { throw new RuntimeException("Could not start selector"); }

    commThread = new TCCommThread(this);
    commThread.start();
  }

  protected void stopImpl() {
    try {
      if (selector != null) {
        selector.wakeup();
      }
    } catch (Exception e) {
      logger.error("Exception trying to stop TCComm", e);
    }
  }

  void addSelectorTask(final Runnable task) {
    Assert.eval(!isCommThread());
    boolean isInterrupted = false;

    try {
      while (true) {
        try {
          selectorTasks.put(task);
          break;
        } catch (InterruptedException e) {
          logger.warn(e);
          isInterrupted = true;
        }
      }
    } finally {
      selector.wakeup();
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  void stopListener(final ServerSocketChannel ssc, final Runnable callback) {
    if (!isCommThread()) {
      Runnable task = new Runnable() {
        public void run() {
          TCCommJDK14.this.stopListener(ssc, callback);
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

  void unregister(SelectableChannel channel) {
    Assert.assertTrue(isCommThread());
    SelectionKey key = channel.keyFor(selector);
    if (key != null) {
      key.cancel();
      key.attach(null);
    }
  }

  void cleanupChannel(final Channel ch, final Runnable callback) {
    if (null == ch) {
      // not expected
      logger.warn("null channel passed to cleanupChannel()", new Throwable());
      return;
    }

    if (!isCommThread()) {
      if (logger.isDebugEnabled()) {
        logger.debug("queue'ing channel close operation");
      }

      addSelectorTask(new Runnable() {
        public void run() {
          TCCommJDK14.this.cleanupChannel(ch, callback);
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
          logger.error("Exception trying to clear selection key", e);
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
                logger.error("Exception trying to shutdown socket output: " + e.getMessage());
              }

              try {
                if (!s.isClosed()) {
                  s.close();
                }
              } catch (Exception e) {
                logger.error("Exception trying to close() socket: " + e.getMessage());
              }
            }
          }
        }
      } else if (ch instanceof ServerSocketChannel) {
        ServerSocketChannel ssc = (ServerSocketChannel) ch;

        try {
          ssc.close();
        } catch (Exception e) {
          logger.error("Exception trying to close() server socket" + e.getMessage());
        }
      }

      try {
        ch.close();
      } catch (Exception e) {
        logger.error("Exception trying to close channel", e);
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

  void requestConnectInterest(TCConnectionJDK14 conn, SocketChannel sc) {
    handleRequest(InterestRequest.createSetInterestRequest(sc, conn, SelectionKey.OP_CONNECT));
  }

  void requestReadInterest(TCJDK14ChannelReader reader, ScatteringByteChannel channel) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, reader, SelectionKey.OP_READ));
  }

  void requestWriteInterest(TCJDK14ChannelWriter writer, GatheringByteChannel channel) {
    handleRequest(InterestRequest.createAddInterestRequest((SelectableChannel) channel, writer, SelectionKey.OP_WRITE));
  }

  void requestAcceptInterest(TCListenerJDK14 lsnr, ServerSocketChannel ssc) {
    handleRequest(InterestRequest.createSetInterestRequest(ssc, lsnr, SelectionKey.OP_ACCEPT));
  }

  void removeWriteInterest(TCConnectionJDK14 conn, SelectableChannel channel) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_WRITE));
  }

  void removeReadInterest(TCConnectionJDK14 conn, SelectableChannel channel) {
    handleRequest(InterestRequest.createRemoveInterestRequest(channel, conn, SelectionKey.OP_READ));
  }

  public void closeEvent(TCListenerEvent event) {
    commThread.listenerAdded(event.getSource());
  }

  void listenerAdded(TCListener listener) {
    commThread.listenerAdded(listener);
  }

  private void handleRequest(final InterestRequest req) {
    // ignore the request if we are stopped/stopping
    if (isStopped()) { return; }

    if (isCommThread()) {
      modifyInterest(req);
    } else {
      addSelectorTask(new Runnable() {
        public void run() {
          TCCommJDK14.this.handleRequest(req);
        }
      });
      return;
    }
  }

  void selectLoop() throws IOException {
    Assert.assertNotNull("selector", selector);
    Assert.eval("Not started", isStarted());

    while (true) {
      final int numKeys;
      try {
        numKeys = selector.select();
      } catch (IOException ioe) {
        if (NIOWorkarounds.linuxSelectWorkaround(ioe)) {
          logger.warn("working around Sun bug 4504001");
          continue;
        }
        throw ioe;
      }

      if (isStopped()) {
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
            task = (Runnable) selectorTasks.poll(0);
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

      final Set selectedKeys = selector.selectedKeys();
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
            ((TCJDK14ChannelReader) key.attachment()).doRead((ScatteringByteChannel) key.channel());
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

  private void dispose() {
    if (selector != null) {

      for (Iterator keys = selector.keys().iterator(); keys.hasNext();) {
        try {
          SelectionKey key = (SelectionKey) keys.next();
          cleanupChannel(key.channel(), null);
        }

        catch (Exception e) {
          logger.warn("Exception trying to close channel", e);
        }
      }

      try {
        selector.close();
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

    // drop any old selector tasks
    selectorTasks = new LinkedQueue();
  }

  private boolean isCommThread() {
    return isCommThread(Thread.currentThread());
  }

  private boolean isCommThread(Thread thread) {
    if (thread == null) { return false; }
    return thread == commThread;
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

        conn.fireErrorEvent(errMsg);
      }
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("IOException attempting to finish socket connection", ioe);
      }

      conn.fireErrorEvent(ioe, null);
    }
  }

  private void modifyInterest(InterestRequest request) {
    Assert.eval(isCommThread());

    try {
      final int existingOps;

      SelectionKey key = request.channel.keyFor(selector);
      if (key != null) {
        existingOps = key.interestOps();
      } else {
        existingOps = 0;
      }

      if (logger.isDebugEnabled()) {
        logger.debug(request);
      }

      if (request.add) {
        request.channel.register(selector, existingOps | request.interestOps, request.attachment);
      } else if (request.set) {
        request.channel.register(selector, request.interestOps, request.attachment);
      } else if (request.remove) {
        request.channel.register(selector, existingOps ^ request.interestOps, request.attachment);
      } else {
        throw new TCInternalError();
      }
    } catch (ClosedChannelException cce) {
      logger.warn("Exception trying to process interest request: " + cce);

    } catch (CancelledKeyException cke) {
      logger.warn("Exception trying to process interest request: " + cke);
    }
  }

  private void doAccept(final SelectionKey key) {
    Assert.eval(isCommThread());

    SocketChannel sc = null;

    TCListenerJDK14 lsnr = (TCListenerJDK14) key.attachment();

    try {
      final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
      sc = ssc.accept();
      sc.configureBlocking(false);
      final Socket s = sc.socket();

      try {
        s.setSendBufferSize(64 * 1024);
      } catch (IOException ioe) {
        logger.warn("IOException trying to setSendBufferSize()");
      }

      try {
        s.setTcpNoDelay(true);
      } catch (IOException ioe) {
        logger.warn("IOException trying to setTcpNoDelay()", ioe);
      }

      TCConnectionJDK14 conn = lsnr.createConnection(sc);
      sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, conn);
    } catch (IOException ioe) {
      if (logger.isInfoEnabled()) {
        logger.info("IO Exception accepting new connection", ioe);
      }

      cleanupChannel(sc, null);
    }
  }

  private Selector     selector;
  private TCCommThread commThread    = null;
  private LinkedQueue  selectorTasks = new LinkedQueue();

  private static class InterestRequest {
    final SelectableChannel channel;
    final Object            attachment;
    final boolean           set;
    final boolean           add;
    final boolean           remove;
    final int               interestOps;

    static InterestRequest createAddInterestRequest(SelectableChannel channel, Object attachment, int interestOps) {
      return new InterestRequest(channel, attachment, interestOps, false, true, false);
    }

    static InterestRequest createSetInterestRequest(SelectableChannel channel, Object attachment, int interestOps) {
      return new InterestRequest(channel, attachment, interestOps, true, false, false);
    }

    static InterestRequest createRemoveInterestRequest(SelectableChannel channel, Object attachment, int interestOps) {
      return new InterestRequest(channel, attachment, interestOps, false, false, true);
    }

    private InterestRequest(SelectableChannel channel, Object attachment, int interestOps, boolean set, boolean add,
                            boolean remove) {
      Assert.eval(remove ^ set ^ add);
      Assert.eval(channel != null);

      this.channel = channel;
      this.attachment = attachment;
      this.set = set;
      this.add = add;
      this.remove = remove;
      this.interestOps = interestOps;
    }

    public String toString() {
      StringBuffer buf = new StringBuffer();

      buf.append("Interest modify request: ").append(channel.toString()).append("\n");
      buf.append("Ops: ");

      if ((interestOps & SelectionKey.OP_ACCEPT) != 0) {
        buf.append(" ACCEPT");
      }

      if ((interestOps & SelectionKey.OP_CONNECT) != 0) {
        buf.append(" CONNECT");
      }

      if ((interestOps & SelectionKey.OP_READ) != 0) {
        buf.append(" READ");
      }

      if ((interestOps & SelectionKey.OP_WRITE) != 0) {
        buf.append(" WRITE");
      }

      buf.append("\n");

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

  // Little helper class to drive the selector. The main point of this class
  // is to isolate the try/finally block around the entire selection process
  private static class TCCommThread extends Thread {
    final TCCommJDK14  commInstance;
    final Set          listeners = new HashSet();
    final int          number    = getNextCounter();
    final String       baseName  = "TCComm Selector Thread " + number;

    private static int counter   = 1;

    private static synchronized int getNextCounter() {
      return counter++;
    }

    TCCommThread(TCCommJDK14 comm) {
      commInstance = comm;
      setDaemon(true);
      setName(baseName);

      if (logger.isDebugEnabled()) {
        logger.debug("Creating a new selector thread (" + toString() + ")", new Throwable());
      }
    }

    String makeListenString(TCListener listener) {
      StringBuffer buf = new StringBuffer();
      buf.append("(listen ");
      buf.append(listener.getBindAddress().getHostAddress());
      buf.append(':');
      buf.append(listener.getBindPort());
      buf.append(')');
      return buf.toString();
    }

    synchronized void listenerRemoved(TCListener listener) {
      listeners.remove(makeListenString(listener));
      updateThreadName();
    }

    synchronized void listenerAdded(TCListener listener) {
      listeners.add(makeListenString(listener));
      updateThreadName();
    }

    private void updateThreadName() {
      StringBuffer buf = new StringBuffer(baseName);
      for (final Iterator iter = listeners.iterator(); iter.hasNext();) {
        buf.append(' ');
        buf.append(iter.next());
      }

      setName(buf.toString());
    }

    public void run() {
      try {
        commInstance.selectLoop();
      } catch (Throwable t) {
        logger.error("Unhandled exception from selectLoop", t);
        t.printStackTrace();
      } finally {
        commInstance.dispose();
      }
    }
  }

}
