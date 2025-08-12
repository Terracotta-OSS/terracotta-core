/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.core;

import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCDirectByteBufferCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.core.event.TCListenerEvent;
import com.tc.net.core.event.TCListenerEventListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import java.util.concurrent.Future;

/**
 * The {@link TCConnectionManager} implementation.
 * 
 * @author teck
 * @author mgovinda
 */
public class TCConnectionManagerImpl implements TCConnectionManager {
  protected static final TCConnection[] EMPTY_CONNECTION_ARRAY = new TCConnection[] {};
  protected static final TCListener[]   EMPTY_LISTENER_ARRAY   = new TCListener[] {};
  private static final boolean                  MESSAGE_PACKUP             = TCPropertiesImpl
                                                                                .getProperties()
                                                                                .getBoolean(TCPropertiesConsts.TC_MESSAGE_PACKUP_ENABLED,
                                                                                            false);
  protected static final Logger logger                 = LoggerFactory.getLogger(TCConnectionManager.class);

  private final TCCommImpl              comm;
  private final Set<TCConnectionImpl>       connections            = new HashSet<>();
  private final Set<TCListener>         listeners              = new HashSet<>();
  private final SetOnceFlag             shutdown               = new SetOnceFlag();
  private final ConnectionEvents        connEvents;
  private final ListenerEvents          listenerEvents;
  private final SocketParams            socketParams;
  private final SocketEndpointFactory    socketEndpointFactory;

  private final TCDirectByteBufferCache buffers = new TCDirectByteBufferCache(TCByteBufferFactory.getFixedBufferSize(), 16 * 1024);

  public TCConnectionManagerImpl() {
    this("ConnectionMgr", null, 0, new ClearTextSocketEndpointFactory());
  }

  public TCConnectionManagerImpl(String name, TCConnectionEventListener listener, int workerCommCount, SocketEndpointFactory socketEndpointFactory) {
    this.connEvents = new ConnectionEvents(listener);
    this.listenerEvents = new ListenerEvents();
    this.socketParams = new SocketParams();
    this.socketEndpointFactory = socketEndpointFactory;
    this.comm = new TCCommImpl(name, workerCommCount, socketParams);
    this.comm.start();
  }
  
  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> state = new LinkedHashMap<>();
    synchronized(connections) {
      state.put("connections", connections.stream().map(connection->connection.getState()).collect(Collectors.toList()));
    }
    state.put("processors", comm.getState());
    state.put("buffers.cached", buffers.size());
    state.put("buffers.referenced", buffers.referenced());
    if (socketEndpointFactory instanceof PrettyPrintable) {
      state.put("bufferManager", ((PrettyPrintable)socketEndpointFactory).getStateMap());
    } else {
      state.put("bufferManager", socketEndpointFactory.toString());
    }
    return state;
  }

  protected TCConnectionImpl createConnectionImpl(TCProtocolAdaptor adaptor, TCConnectionEventListener listener) {
    return new TCConnectionImpl(listener, adaptor, this, comm.nioServiceThreadForNewConnection(), socketParams, socketEndpointFactory);
  }

  @SuppressWarnings("resource")
  protected TCListener createListenerImpl(InetSocketAddress addr, ProtocolAdaptorFactory factory, int backlog,
                                          boolean reuseAddr) throws IOException {
    ServerSocketChannel ssc = ServerSocketChannel.open();
    ssc.configureBlocking(false);
    ServerSocket serverSocket = ssc.socket();
    this.socketParams.applyServerSocketParams(serverSocket, reuseAddr);

    try {
      serverSocket.bind(addr, backlog);
    } catch (IOException ioe) {
      logger.warn("Unable to bind socket on address " + addr.getAddress() + ", port " + addr.getPort() + ", "
                  + ioe.getMessage());
      throw ioe;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Bind: " + serverSocket.getLocalSocketAddress());
    }

    CoreNIOServices commThread = comm.nioServiceThreadForNewListener();

    TCListenerImpl rv = new TCListenerImpl(ssc, factory, getConnectionListener(), this, commThread, socketEndpointFactory);

    commThread.registerListener(rv, ssc);

    return rv;
  }

  @Override
  public TCConnection[] getAllConnections() {
    synchronized (connections) {
      return connections.toArray(EMPTY_CONNECTION_ARRAY);
    }
  }

  @Override
  public synchronized TCListener[] getAllListeners() {
    return listeners.toArray(EMPTY_LISTENER_ARRAY);
  }

  @Override
  public final TCListener createListener(InetSocketAddress addr, ProtocolAdaptorFactory factory)
      throws IOException {
    return createListener(addr, factory, Constants.DEFAULT_ACCEPT_QUEUE_DEPTH, true);
  }

  @Override
  public final TCListener createListener(InetSocketAddress addr, ProtocolAdaptorFactory factory,
                                                      int backlog, boolean reuseAddr) throws IOException {
    checkShutdown();

    TCListener rv = createListenerImpl(addr, factory, backlog, reuseAddr);
    rv.addEventListener(listenerEvents);

    addCreatedListener(rv);

    return rv;
  }

  @Override
  public final TCConnection createConnection(TCProtocolAdaptor adaptor) throws IOException {
    checkShutdown();

    TCConnectionImpl rv = createConnectionImpl(adaptor, connEvents);
    newConnection(rv);

    return rv;
  }

  @Override
  public void closeAllConnections() {
    closeAllConnections(false);
  }
  
  @Override
  public void asynchCloseAllConnections() {
    closeAllConnections(true);
  }

  private void closeAllConnections(boolean async) {
    TCConnection[] conns = getAllConnections();

    logger.info("closing {} connection/s for {}", conns.length, this.comm.toString());
    for (TCConnection conn : conns) {
      try {
        Future<Void> c = conn.asynchClose();
        if (!async) {
          c.get();
        }
      } catch (Exception e) {
        logger.error("Exception trying to close " + conn, e);
      }
    }
  }

  @Override
  public void closeAllListeners() {    
    TCListener[] list = getAllListeners();
    for (TCListener lsnr : list) {
      try {
        lsnr.stop();
      } catch (Exception e) {
        logger.error("Exception trying to close " + lsnr, e);
      }
    }
  }
  
  private synchronized void addCreatedListener(TCListener listener) throws IOException {
    if (listener.isStopped()) {
      throw new IOException("listener closed");
    }
    listeners.add(listener);
  }
  
  private synchronized void removeDeadListener(TCListener listener) {
    if (!listener.isStopped()) {
      throw new RuntimeException("listener not closed");
    }
    listeners.remove(listener);
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
      this.buffers.close();
      comm.stop();
    }
  }

  void connectionClosed(TCConnection conn) {
    synchronized (connections) {
      connections.remove(conn);
    }
  }

  void newConnection(TCConnectionImpl conn) {
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

  private void checkShutdown() throws IOException {
    if (shutdown.isSet()) { throw new IOException("connection manager shutdown"); }
  }

  static class ConnectionEvents implements TCConnectionEventListener {
    private final TCConnectionEventListener subl;

    public ConnectionEvents(TCConnectionEventListener subl) {
      this.subl = subl;
    }

    @Override
    public final void connectEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("connect event: " + event.toString());
      }
      if (subl != null) {
        subl.connectEvent(event);
      }
    }

    @Override
    public final void closeEvent(TCConnectionEvent event) {
      if (logger.isDebugEnabled()) {
        logger.debug("close event: " + event.toString());
      }
      if (subl != null) {
        subl.closeEvent(event);
      }
    }

    @Override
    public final void errorEvent(TCConnectionErrorEvent event) {
      try {
        final Throwable err = event.getException();
        if (err != null) {
          if (err instanceof IOException) {
            if (!event.getSource().isClosed()) {
              logger.info("error event on connection " + event.getSource() + ": " + err.getMessage());
            } else if (logger.isDebugEnabled()) {
              logger.debug("error event on connection " + event.getSource() + ": " + err.getMessage(), err);
            }
          } else {
            logger.error("Exception: ", err);
          }
          if (subl != null) {
            subl.errorEvent(event);
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
      if (subl != null) {
        subl.endOfFileEvent(event);
      }
      event.getSource().asynchClose();
    }
  }

  class ListenerEvents implements TCListenerEventListener {
    @Override
    public void closeEvent(TCListenerEvent event) {
      removeDeadListener(event.getSource());
    }
  }

  TCDirectByteBufferCache getBufferCache() {
    return buffers;
  }
  
  @Override
  public int getBufferCount() {
    return buffers.referenced();
  }
  
  void distribute() {
    connections.forEach(TCConnectionImpl::migrate);
  }
}
