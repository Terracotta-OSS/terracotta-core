/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.TCExceptionResultException;
import com.tc.util.concurrent.TCFuture;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TCListener implementation
 * 
 * @author teck
 */
final class TCListenerImpl implements TCListener {
  protected final static TCLogger         logger          = TCLogging.getLogger(TCListener.class);

  private final ServerSocketChannel       ssc;
  private final TCConnectionEventListener listener;
  private final TCConnectionManagerImpl   parent;
  private final InetAddress               addr;
  private final int                       port;
  private final TCSocketAddress           sockAddr;
  private final TCListenerEvent           staticEvent;
  private final SetOnceFlag               closeEventFired = new SetOnceFlag();
  private final SetOnceFlag               stopPending     = new SetOnceFlag();
  private final SetOnceFlag               stopped         = new SetOnceFlag();
  private final CopyOnWriteArraySet       listeners       = new CopyOnWriteArraySet();
  private final ProtocolAdaptorFactory    factory;
  private final CoreNIOServices           commNIOServiceThread;

  TCListenerImpl(ServerSocketChannel ssc, ProtocolAdaptorFactory factory, TCConnectionEventListener listener,
                 TCConnectionManagerImpl managerJDK14, CoreNIOServices commNIOServiceThread) {
    this.addr = ssc.socket().getInetAddress();
    this.port = ssc.socket().getLocalPort();
    this.sockAddr = new TCSocketAddress(this.addr, this.port);
    this.factory = factory;
    this.staticEvent = new TCListenerEvent(this);
    this.ssc = ssc;
    this.listener = listener;
    this.parent = managerJDK14;
    this.commNIOServiceThread = commNIOServiceThread;
  }

  protected void stopImpl(Runnable callback) {
    commNIOServiceThread.stopListener(ssc, callback);
  }

  TCConnectionImpl createConnection(SocketChannel ch, CoreNIOServices nioServiceThread, SocketParams socketParams)
      throws IOException {
    TCProtocolAdaptor adaptor = getProtocolAdaptorFactory().getInstance();
    TCConnectionImpl rv = new TCConnectionImpl(listener, adaptor, ch, parent, nioServiceThread, socketParams);
    rv.finishConnect();
    parent.newConnection(rv);
    return rv;
  }

  public final void stop() {
    try {
      stop(0);
    } catch (Exception e) {
      logger.error("unexpected exception", e);
    }
  }

  public final TCSocketAddress getBindSocketAddress() {
    return sockAddr;
  }

  public final void stop(long timeout) throws TCTimeoutException {
    if (stopped.isSet()) {
      logger.warn("listener already stopped");
      return;
    }

    if (stopPending.attemptSet()) {
      final TCFuture future = new TCFuture();

      stopImpl(new Runnable() {
        public void run() {
          future.set("stop done");
        }
      });

      try {
        future.get(timeout);
      } catch (InterruptedException e) {
        logger.warn("stop interrupted");
        Thread.currentThread().interrupt();
        return;
      } catch (TCExceptionResultException e) {
        logger.error(e);
        Assert.eval("exception result set in future", false);
        return;
      } finally {
        fireCloseEvent();
        stopped.set();
      }
    } else {
      logger.warn("stop already requested");
    }
  }

  public final int getBindPort() {
    return port;
  }

  public final InetAddress getBindAddress() {
    return addr;
  }

  public final void addEventListener(TCListenerEventListener lsnr) {
    if (lsnr == null) {
      logger.warn("trying to add a null event listener");
      return;
    }

    listeners.add(lsnr);
  }

  public final void removeEventListener(TCListenerEventListener lsnr) {
    if (lsnr == null) {
      logger.warn("trying to remove a null event listener");
      return;
    }

    listeners.remove(lsnr);
  }

  public final boolean isStopped() {
    return stopped.isSet();
  }

  @Override
  public final String toString() {
    return getClass().getName() + " " + addr.getHostAddress() + ":" + port;
  }

  protected final void fireCloseEvent() {
    if (closeEventFired.attemptSet()) {
      for (final Iterator iter = listeners.iterator(); iter.hasNext();) {
        final TCListenerEventListener lsnr = (TCListenerEventListener) iter.next();

        try {
          lsnr.closeEvent(staticEvent);
        } catch (Exception e) {
          logger.error("exception in close event handler", e);
        }
      }
    }
  }

  final ProtocolAdaptorFactory getProtocolAdaptorFactory() {
    return factory;
  }
}