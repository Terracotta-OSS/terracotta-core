/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.core;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.transport.ConnectionHealthCheckerUtil;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The {@link TCConnectionManager} implementation.
 * 
 * @author teck
 * @author mgovinda
 */
public class TCConnectionManagerImpl implements TCConnectionManager {
  protected static final TCConnection[] EMPTY_CONNECTION_ARRAY = new TCConnection[] {};
  protected static final TCListener[]   EMPTY_LISTENER_ARRAY   = new TCListener[] {};
  protected static final TCLogger       logger                 = TCLogging.getLogger(TCConnectionManager.class);

  private final TCCommImpl              comm;
  private final HealthCheckerConfig     healthCheckerConfig;
  private final Set                     connections            = new HashSet();
  private final Set                     listeners              = new HashSet();
  private final SetOnceFlag             shutdown               = new SetOnceFlag();
  private final ConnectionEvents        connEvents;
  private final ListenerEvents          listenerEvents;
  private final SocketParams            socketParams;
  private final TCSecurityManager       securityManager;

  public TCConnectionManagerImpl() {
    this("ConnectionMgr", 0, new HealthCheckerConfigImpl("DefaultConfigForActiveConnections"), null);
  }

  public TCConnectionManagerImpl(String name, int workerCommCount, HealthCheckerConfig healthCheckerConfig,
                                 TCSecurityManager securityManager) {
    this.securityManager = securityManager;
    this.connEvents = new ConnectionEvents();
    this.listenerEvents = new ListenerEvents();
    this.socketParams = new SocketParams();
    this.healthCheckerConfig = healthCheckerConfig;
    this.comm = new TCCommImpl(name, workerCommCount, socketParams);
    this.comm.start();
  }

  protected TCConnection createConnectionImpl(TCProtocolAdaptor adaptor, TCConnectionEventListener listener) {
    return new TCConnectionImpl(listener, adaptor, this, comm.nioServiceThreadForNewConnection(), socketParams, securityManager);
  }

  protected TCListener createListenerImpl(TCSocketAddress addr, ProtocolAdaptorFactory factory, int backlog,
                                          boolean reuseAddr) throws IOException {
    ServerSocketChannel ssc = ServerSocketChannel.open();
    ssc.configureBlocking(false);
    ServerSocket serverSocket = ssc.socket();
    this.socketParams.applyServerSocketParams(serverSocket, reuseAddr);

    try {
      serverSocket.bind(new InetSocketAddress(addr.getAddress(), addr.getPort()), backlog);
    } catch (IOException ioe) {
      logger.warn("Unable to bind socket on address " + addr.getAddress() + ", port " + addr.getPort() + ", "
                  + ioe.getMessage());
      throw ioe;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Bind: " + serverSocket.getLocalSocketAddress());
    }

    CoreNIOServices commThread = comm.nioServiceThreadForNewListener();

    TCListenerImpl rv = new TCListenerImpl(ssc, factory, getConnectionListener(), this, commThread, securityManager);

    commThread.registerListener(rv, ssc);

    return rv;
  }

  @Override
  public TCConnection[] getAllConnections() {
    synchronized (connections) {
      return (TCConnection[]) connections.toArray(EMPTY_CONNECTION_ARRAY);
    }
  }

  /**
   * Connection is active if and only if it is Transport Established and the idle time is less than the HC max idle
   * time.
   */
  @Override
  public TCConnection[] getAllActiveConnections() {
    synchronized (connections) {
      ArrayList activeConnections = new ArrayList();
      long maxIdleTime = ConnectionHealthCheckerUtil.getMaxIdleTimeForAlive(healthCheckerConfig, false);
      for (Iterator i = connections.iterator(); i.hasNext();) {
        TCConnection conn = (TCConnection) i.next();
        if ((conn.getIdleTime() < maxIdleTime) && conn.isTransportEstablished()) {
          activeConnections.add(conn);
        } else {
          logger.info(conn + "  is not active; Max allowed Idle time:" + maxIdleTime + "; Transport Established: "
                      + conn.isTransportEstablished());
        }
      }
      logger.info("Active connections : " + activeConnections.size() + " out of " + connections.size());
      return (TCConnection[]) activeConnections.toArray(new TCConnection[activeConnections.size()]);
    }
  }

  @Override
  public TCListener[] getAllListeners() {
    synchronized (listeners) {
      return (TCListener[]) listeners.toArray(EMPTY_LISTENER_ARRAY);
    }
  }

  @Override
  public final synchronized TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory)
      throws IOException {
    return createListener(addr, factory, Constants.DEFAULT_ACCEPT_QUEUE_DEPTH, true);
  }

  @Override
  public final synchronized TCListener createListener(TCSocketAddress addr, ProtocolAdaptorFactory factory,
                                                      int backlog, boolean reuseAddr) throws IOException {
    checkShutdown();

    TCListener rv = createListenerImpl(addr, factory, backlog, reuseAddr);
    rv.addEventListener(listenerEvents);

    synchronized (listeners) {
      listeners.add(rv);
    }

    return rv;
  }

  @Override
  public final synchronized TCConnection createConnection(TCProtocolAdaptor adaptor) {
    checkShutdown();

    TCConnection rv = createConnectionImpl(adaptor, connEvents);
    newConnection(rv);

    return rv;
  }

  @Override
  public synchronized void closeAllConnections(long timeout) {
    closeAllConnections(false, timeout);
  }

  @Override
  public synchronized void asynchCloseAllConnections() {
    closeAllConnections(true, 0);
  }

  private void closeAllConnections(boolean async, long timeout) {
    TCConnection[] conns;

    synchronized (connections) {
      conns = (TCConnection[]) connections.toArray(EMPTY_CONNECTION_ARRAY);
    }

    for (TCConnection conn : conns) {
      try {
        if (async) {
          conn.asynchClose();
        } else {
          conn.close(timeout);
        }
      } catch (Exception e) {
        logger.error("Exception trying to close " + conn, e);
      }
    }
  }

  @Override
  public synchronized void closeAllListeners() {
    TCListener[] list;

    synchronized (listeners) {
      list = (TCListener[]) listeners.toArray(EMPTY_LISTENER_ARRAY);
    }

    for (TCListener lsnr : list) {
      try {
        lsnr.stop();
      } catch (Exception e) {
        logger.error("Exception trying to close " + lsnr, e);
      }
    }
  }

  @Override
  public TCComm getTcComm() {
    return this.comm;
  }

  @Override
  public final synchronized void shutdown() {
    if (shutdown.attemptSet()) {
      closeAllListeners();
      asynchCloseAllConnections();
      comm.stop();
    }
  }

  void connectionClosed(TCConnection conn) {
    synchronized (connections) {
      connections.remove(conn);
    }
  }

  void newConnection(TCConnection conn) {
    synchronized (connections) {
      connections.add(conn);
    }
  }

  void removeConnection(TCConnection connection) {
    synchronized (connections) {
      connections.remove(connection);
    }
  }

  protected TCConnectionEventListener getConnectionListener() {
    return connEvents;
  }

  private final void checkShutdown() {
    if (shutdown.isSet()) { throw new IllegalStateException("connection manager shutdown"); }
  }

  static class ConnectionEvents implements TCConnectionEventListener {
    @Override
    public final void connectEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("connect event: " + event.toString());
      }
    }

    @Override
    public final void closeEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("close event: " + event.toString());
      }
    }

    @Override
    public final void errorEvent(TCConnectionErrorEvent event) {
      try {
        final Throwable err = event.getException();

        if (err != null) {
          if (err instanceof IOException) {
            if (logger.isInfoEnabled()) {
              logger.info("error event on connection " + event.getSource() + ": " + err.getMessage());
            } else if (logger.isDebugEnabled()) {
              logger.debug("error event on connection " + event.getSource() + ": " + err.getMessage(), err);
            }
          } else {
            logger.error(err);
          }
        }
      } finally {
        event.getSource().asynchClose();
      }
    }

    @Override
    public final void endOfFileEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("EOF event: " + event.toString());
      }

      event.getSource().asynchClose();
    }
  }

  class ListenerEvents implements TCListenerEventListener {
    @Override
    public void closeEvent(TCListenerEvent event) {
      synchronized (listeners) {
        listeners.remove(event.getSource());
      }
    }
  }

}
