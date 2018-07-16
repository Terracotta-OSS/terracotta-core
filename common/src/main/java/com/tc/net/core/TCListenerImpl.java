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
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TCListener implementation
 * 
 * @author teck
 */
final class TCListenerImpl implements TCListener {
  protected final static Logger logger = LoggerFactory.getLogger(TCListener.class);

  private final ServerSocketChannel                          ssc;
  private final TCConnectionEventListener                    listener;
  private final TCConnectionManagerImpl                      parent;
  private final InetAddress                                  addr;
  private final int                                          port;
  private final TCSocketAddress                              sockAddr;
  private final TCListenerEvent                              staticEvent;
  private final SetOnceFlag                                  closeEventFired = new SetOnceFlag();
  private final SetOnceFlag                                  stopPending     = new SetOnceFlag();
  private final SetOnceFlag                                  stopped         = new SetOnceFlag();
  private final CopyOnWriteArraySet<TCListenerEventListener> listeners       = new CopyOnWriteArraySet<TCListenerEventListener>();
  private final ProtocolAdaptorFactory                       factory;
  private final CoreNIOServices                              commNIOServiceThread;
  private final BufferManagerFactory                         bufferManagerFactory;

  TCListenerImpl(ServerSocketChannel ssc, ProtocolAdaptorFactory factory, TCConnectionEventListener listener,
                 TCConnectionManagerImpl managerJDK14, CoreNIOServices commNIOServiceThread, BufferManagerFactory bufferManagerFactory) {
    this.addr = ssc.socket().getInetAddress();
    this.port = ssc.socket().getLocalPort();
    this.bufferManagerFactory = bufferManagerFactory;
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
    TCConnectionImpl rv = new TCConnectionImpl(listener, adaptor, ch, parent, nioServiceThread, socketParams, bufferManagerFactory);
    rv.finishConnect();
    parent.newConnection(rv);
    return rv;
  }

  @Override
  public final void stop() {
    try {
      stop(0);
    } catch (Exception e) {
      logger.error("unexpected exception", e);
    }
  }

  @Override
  public final TCSocketAddress getBindSocketAddress() {
    return sockAddr;
  }

  @Override
  public final void stop(long timeout) throws TCTimeoutException {
    if (stopped.isSet()) {
      logger.warn("listener already stopped");
      return;
    }

    if (stopPending.attemptSet()) {
      final TCFuture future = new TCFuture();

      stopImpl(new Runnable() {
        @Override
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
        logger.error("Exception: ", e);
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

  @Override
  public final int getBindPort() {
    return port;
  }

  @Override
  public final InetAddress getBindAddress() {
    return addr;
  }

  @Override
  public final void addEventListener(TCListenerEventListener lsnr) {
    if (lsnr == null) {
      logger.warn("trying to add a null event listener");
      return;
    }

    listeners.add(lsnr);
  }

  @Override
  public final void removeEventListener(TCListenerEventListener lsnr) {
    if (lsnr == null) {
      logger.warn("trying to remove a null event listener");
      return;
    }

    listeners.remove(lsnr);
  }

  @Override
  public final boolean isStopped() {
    return stopped.isSet();
  }

  @Override
  public final String toString() {
    return getClass().getName() + " " + addr.getHostAddress() + ":" + port;
  }

  protected final void fireCloseEvent() {
    if (closeEventFired.attemptSet()) {
      for (TCListenerEventListener lsnr : listeners) {
        
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
