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
package com.tc.net.protocol.transport;

import org.slf4j.Logger;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.core.ProductID;
import com.tc.util.Assert;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of MessaageTransport
 */
abstract class MessageTransportBase extends AbstractMessageTransport implements TCConnectionEventListener {
  private volatile TCConnection                             connection;

  private ConnectionID                           connectionId           = new ConnectionID(JvmIDUtil.getJvmID(),
                                                                                             ChannelID.NULL_ID.toLong());
  protected final MessageTransportStatus           status;
  protected final TransportHandshakeMessageFactory messageFactory;
  private final TransportHandshakeErrorHandler     handshakeErrorHandler;
  private WeakReference<NetworkLayer>                             receiveLayer;

  private final AtomicReference<TCConnectionEvent> connectionCloseEvent   = new AtomicReference<>();
  private volatile ConnectionHealthCheckerContext  healthCheckerContext   = new ConnectionHealthCheckerContextDummyImpl();
  private int                                      remoteCallbackPort     = TransportHandshakeMessage.NO_CALLBACK_PORT;

  protected MessageTransportBase(MessageTransportState initialState,
                                 TransportHandshakeErrorHandler handshakeErrorHandler,
                                 TransportHandshakeMessageFactory messageFactory, Logger logger) {

    super(logger);
    this.handshakeErrorHandler = handshakeErrorHandler;
    this.messageFactory = messageFactory;
    this.status = new MessageTransportStatus(initialState, logger);
  }

  public synchronized void setHealthCheckerContext(ConnectionHealthCheckerContext context) {
    healthCheckerContext = context;
  }

  public synchronized ConnectionHealthCheckerContext getHealthCheckerContext() {
    return healthCheckerContext;
  }

  @Override
  public final ConnectionID getConnectionID() {
    return this.connectionId;
  }

  @Override
  public ProductID getProductID() {
    return this.connectionId.getProductId();
  }
  
  @Override
  public final void setReceiveLayer(NetworkLayer layer) {
    this.receiveLayer = new WeakReference<>(layer);
  }

  @Override
  public final NetworkLayer getReceiveLayer() {
    return receiveLayer == null ? null : receiveLayer.get();
  }

  @Override
  public final void setSendLayer(NetworkLayer layer) {
    throw new UnsupportedOperationException("Transport layer has no send layer.");
  }

  private boolean isSameConnection(TCConnection checkConn) {
    return checkConn == this.getConnection();
  }

  @Override
  public final void receiveTransportMessage(WireProtocolMessage message) {
    if (isSameConnection(message.getSource())) {
      receiveTransportMessageImpl(message);
    } else {
      getLogger().warn("Received message from an old connection: " + message.getSource() + "; " + message);
    }
  }

  protected abstract void receiveTransportMessageImpl(WireProtocolMessage message);

  protected final void receiveToReceiveLayer(WireProtocolMessage message) {
    NetworkLayer receiver = this.getReceiveLayer();
    if (receiver == null) {
      disconnect();
      return;
    } else if (message.getMessageProtocol() == WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE) {
      // message is printed for debugging
      getLogger().info(message.toString());
      throw new AssertionError("Wrong handshake message from: " + message.getSource());
    } else if (message.getMessageProtocol() == WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES) {
      if (this.healthCheckerContext.receiveProbe((HealthCheckerProbeMessage) message)) {
        return;
      } else {
        throw new AssertionError("Wrong HealthChecker Probe message from: " + message.getSource());
      }
    }
    receiver.receive(message.getPayload());
  }

  @Override
  public final void receive(TCByteBuffer[] msgData) {
    throw new UnsupportedOperationException();
  }

  /**
   * Moves the MessageTransport state to closed and closes the underlying connection, if any.
   */
  @Override
  public void close() {
    terminate(false);
  }

  public void disconnect() {
    terminate(true);
  }
  
  protected boolean resetIfNotEnd() {
    return this.status.resetIfNotEnd();
  }

  private void terminate(boolean disconnect) {
    synchronized (this.status) {
      if (status.isEnd()) {
        getLogger().debug("Can only close an open connection");
        return;
      } else if (disconnect) {
        this.status.disconnect();
        // Dont fire any events here. Anyway asynchClose is triggered below and we are expected to receive a closeEvent
        // and upon which we open up the OOO Reconnect window
      } else {
        this.status.end();
      }
    }    
    
    if (!disconnect) {
      fireTransportClosedEvent();
    }
    if (healthCheckerContext != null) {
      this.healthCheckerContext.close();
    }
    if (connection != null && !this.connection.isClosed()) {
      this.connection.asynchClose();
    }
  }

  @Override
  public final void send(TCNetworkMessage message) throws IOException {
    if (status.isEstablished()) {
      sendToConnection(message);
    } else {
      throw new IOException("connection not established");
    }
  }

  // Do not override this method. Not a final method, as a test class is deriving it
  @Override
  public void sendToConnection(TCNetworkMessage message) throws IOException {
    if (message == null) throw new AssertionError("Attempt to send a null message.");
    if (!status.isEnd()) {
      connection.putMessage(message);
    } else {
      throw new IOException("Couldn't send message status: " + status);
    }
  }

  /**
   * Returns true if the underlying connection is open.
   */
  @Override
  public boolean isConnected() {
    TCConnection conn = getConnection();
    return (conn != null && conn.isConnected() && conn.isTransportEstablished() && !conn.isClosed() && !conn.isClosePending());
  }

  @Override
  public final void attachNewConnection(TCConnection newConnection) throws IllegalReconnectException {
    getConnectionAttacher().attachNewConnection(this.connectionCloseEvent.getAndSet(null), this.connection,
                                                newConnection);
  }

  protected ConnectionAttacher getConnectionAttacher() {
    return new DefaultConnectionAttacher(this, getLogger());
  }

  protected interface ConnectionAttacher {
    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection);
  }

  private static final class DefaultConnectionAttacher implements ConnectionAttacher {

    private final MessageTransportBase transport;
    private final Logger logger;

    private DefaultConnectionAttacher(MessageTransportBase transport, Logger logger) {
      this.transport = transport;
      this.logger = logger;
    }

    @Override
    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection) {
      Assert.assertNotNull(oldConnection);
      if (closeEvent == null || closeEvent.getSource() != oldConnection) {
        // We either didn't receive a close event or we received a close event
        // from a connection that isn't our current connection.
        if (transport.isConnected()) {
          // DEV-1689 : Don't bother for connections which actually didn't make up to Transport Establishment.
          this.transport.status.reset();
          this.transport.fireTransportDisconnectedEvent();
          this.transport.getConnection().asynchClose();
        } else {
          logger.warn("Old connection " + oldConnection + "might not have been Transport Established ");
        }
      }
      // remove the transport as a listener for the old connection
      if (oldConnection != null && oldConnection != transport.getConnection()) {
        oldConnection.removeListener(transport);
      }
      // set the new connection to the current connection.
      transport.wireNewConnection(newConnection);
    }
  }

  /*********************************************************************************************************************
   * TCConnection listener interface
   */

  @Override
  public void connectEvent(TCConnectionEvent event) {
    status.connected();
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    if (isSameConnection(event.getSource())) {
      if (this.connectionCloseEvent.compareAndSet(null, event)) {
        boolean forcedDisconnect = false;
        synchronized (status) {
          getLogger().debug("CLOSE EVENT : " + this.connection + ". STATUS : " + status);
          if (status.isEnd()) {
            return;
            // do nothing, connection is already finished
          } else if (status.isConnected() || status.isEstablished() || status.isDisconnected()) {
            if (status.isDisconnected()) {
              forcedDisconnect = true;
            }
            status.reset();
          } else {
            status.reset();
            getLogger().debug("closing down connection - " + event + " - " + status);
            return;
          }
        }

        if (forcedDisconnect) {
          fireTransportForcedDisconnectEvent();
        } else {
          fireTransportDisconnectedEvent();
        }
      }
    } else {
        getLogger().debug("NOT SAME CONNECTION");
    }
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    return;
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {
    return;
  }

  protected void handleHandshakeError(TransportHandshakeErrorContext e) {
    this.handshakeErrorHandler.handleHandshakeError(e);
  }

  protected TCConnection getConnection() {
    return connection;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (connection != null ? this.connection.getRemoteAddress() : null);
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return (connection != null ? this.connection.getLocalAddress() : null);
  }

  protected void setConnection(TCConnection conn) {
    TCConnection old = this.connection;
    this.connection = conn;
    this.connectionCloseEvent.set(null);
    this.connection.addListener(this);
    if (old != null) {
      old.removeListener(this);
    }
  }

  protected void clearConnection() {
    TCConnection conn;
    if ((conn = getConnection()) != null) {
      conn.close(10000);
      conn.removeListener(this);
      this.connection = null;
      resetIfNotEnd();
    }
  }

  protected boolean wireNewConnection(TCConnection conn) {
    synchronized (status) {
      if (this.status.isEnd()) {
        getLogger().warn("Connection stack is already closed. " + this.status + "; Conn: " + conn);
        conn.removeListener(this);
        conn.asynchClose();
        return false;
      } else {
        setConnection(conn);
        this.status.reset();
        //  connection is already connected.  set proper status
        if (conn.isConnected()) {
          this.status.connected();
        }
        return true;
      }
    }
  }

  /**
   * this function gets the stackLayerFlag added to build the communication stack information
   */
  @Override
  public short getStackLayerFlag() {
    // this is the transport layer
    return TYPE_TRANSPORT_LAYER;
  }

  /**
   * This function gets the stack layer name of the present layer added to build the communication stack information
   */
  @Override
  public String getStackLayerName() {
    // this is the transport layer
    return NAME_TRANSPORT_LAYER;
  }

  @Override
  public synchronized int getRemoteCallbackPort() {
    return this.remoteCallbackPort;
  }

  @Override
  public synchronized void setRemoteCallbackPort(int remoteCallbackPort) {
    this.remoteCallbackPort = remoteCallbackPort;
  }

  @Override
  public synchronized final void initConnectionID(ConnectionID cid) {
    connectionId = cid;
  }

  protected synchronized final void clearConnectionID() {
    this.connectionId = new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong());
  }

  void log(String msg) {
    if (!getProductID().isInternal()) {
      getLogger().info(msg);
    } else {
      getLogger().debug(msg);
    }

  }

  @Override
  public Map<String, ?> getStateMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("connection", this.getConnection().getState());
    map.put("id", connectionId.toString());
    return map;
  }
}
