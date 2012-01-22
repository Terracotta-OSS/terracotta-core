/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.ConnectionIDProvider;
import com.tc.logging.TCLogger;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of MessaageTransport
 */
abstract class MessageTransportBase extends AbstractMessageTransport implements NetworkLayer,
    TCConnectionEventListener, ConnectionIDProvider {
  private TCConnection                             connection;

  protected ConnectionID                           connectionId           = new ConnectionID(JvmIDUtil.getJvmID(),
                                                                                             ChannelID.NULL_ID.toLong());
  protected final MessageTransportStatus           status;
  protected final AtomicBoolean                    isOpen;
  protected final TransportHandshakeMessageFactory messageFactory;
  private final TransportHandshakeErrorHandler     handshakeErrorHandler;
  private NetworkLayer                             receiveLayer;

  private final Object                             attachingNewConnection = new Object();
  private final AtomicReference                    connectionCloseEvent   = new AtomicReference(null);
  private boolean                                  allowConnectionReplace = false;
  private volatile ConnectionHealthCheckerContext  healthCheckerContext   = new ConnectionHealthCheckerContextDummyImpl();
  private int                                      remoteCallbackPort     = TransportHandshakeMessage.NO_CALLBACK_PORT;

  protected MessageTransportBase(MessageTransportState initialState,
                                 TransportHandshakeErrorHandler handshakeErrorHandler,
                                 TransportHandshakeMessageFactory messageFactory, boolean isOpen, TCLogger logger) {

    super(logger);
    this.handshakeErrorHandler = handshakeErrorHandler;
    this.messageFactory = messageFactory;
    this.isOpen = new AtomicBoolean(isOpen);
    this.status = new MessageTransportStatus(initialState, logger);
  }

  public void setAllowConnectionReplace(boolean allow) {
    this.allowConnectionReplace = allow;
  }

  public synchronized void setHealthCheckerContext(ConnectionHealthCheckerContext context) {
    healthCheckerContext = context;
  }

  public synchronized ConnectionHealthCheckerContext getHealthCheckerContext() {
    return healthCheckerContext;
  }

  public final ConnectionID getConnectionId() {
    return this.connectionId;
  }

  public final void setReceiveLayer(NetworkLayer layer) {
    this.receiveLayer = layer;
  }

  public final NetworkLayer getReceiveLayer() {
    return receiveLayer;
  }

  public final void setSendLayer(NetworkLayer layer) {
    throw new UnsupportedOperationException("Transport layer has no send layer.");
  }

  public final void receiveTransportMessage(WireProtocolMessage message) {
    synchronized (attachingNewConnection) {
      if (message.getSource() == this.connection) {
        receiveTransportMessageImpl(message);
      } else {
        logger.warn("Received message from an old connection: " + message.getSource() + "; " + message);
      }
    }
  }

  public abstract NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, IOException,
      CommStackMismatchException;

  protected abstract void receiveTransportMessageImpl(WireProtocolMessage message);

  protected final void receiveToReceiveLayer(WireProtocolMessage message) {
    Assert.assertNotNull(receiveLayer);
    if (message.getMessageProtocol() == WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE) {
      // message is printed for debugging
      logger.info(message.toString());
      throw new AssertionError("Wrong handshake message from: " + message.getSource());
    } else if (message.getMessageProtocol() == WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES) {
      if (this.healthCheckerContext.receiveProbe((HealthCheckerProbeMessage) message)) {
        return;
      } else {
        throw new AssertionError("Wrong HealthChecker Probe message from: " + message.getSource());
      }
    }
    this.receiveLayer.receive(message.getPayload());
    message.getWireProtocolHeader().recycle();
  }

  public final void receive(TCByteBuffer[] msgData) {
    throw new UnsupportedOperationException();
  }

  /**
   * Moves the MessageTransport state to closed and closes the underlying connection, if any.
   */
  public void close() {
    terminate(false);
  }

  public void disconnect() {
    terminate(true);
  }

  private void terminate(boolean disconnect) {
    synchronized (isOpen) {
      if (!isOpen.get()) {
        // see DEV-659: we used to throw an assertion error here if already closed
        logger.warn("Can only close an open connection");
        return;
      }
      if (disconnect) {
        synchronized (status) {
          if (!this.status.isEnd()) this.status.disconnect();
        }
        // Dont fire any events here. Anyway asynchClose is triggered below and we are expected to receive a closeEvent
        // and upon which we open up the OOO Reconnect window
      } else {
        synchronized (status) {
          if (!this.status.isEnd()) this.status.closed();
        }
        isOpen.set(false);
        fireTransportClosedEvent();
      }
    }

    synchronized (status) {
      if (connection != null && !this.connection.isClosed()) {
        this.connection.asynchClose();
      }
    }
  }

  public final void send(TCNetworkMessage message) {
    // synchronized (isOpen) {
    // Assert.eval("Can't send on an unopen transport [" +
    // Thread.currentThread().getName() + "]", isOpen.get());
    // }

    synchronized (status) {
      if (!status.isEstablished()) {
        logger.warn("Ignoring message sent to non-established transport: " + message);
        return;
      }

      sendToConnection(message);
    }
  }

  // Do not override this method. Not a final method, as a test class is deriving it
  public void sendToConnection(TCNetworkMessage message) {
    if (message == null) throw new AssertionError("Attempt to send a null message.");
    connection.putMessage(message);
  }

  /**
   * Returns true if the underlying connection is open.
   */
  public final boolean isConnected() {
    synchronized (status) {
      return ((getConnection() != null) && getConnection().isConnected() && this.status.isEstablished());
    }
  }

  public final void attachNewConnection(TCConnection newConnection) throws IllegalReconnectException {
    synchronized (attachingNewConnection) {
      if ((this.connection != null) && !allowConnectionReplace) { throw new IllegalReconnectException(); }

      getConnectionAttacher().attachNewConnection((TCConnectionEvent) this.connectionCloseEvent.get(), this.connection,
                                                  newConnection);
    }
  }

  protected ConnectionAttacher getConnectionAttacher() {
    return new DefaultConnectionAttacher(this, getLogger());
  }

  protected interface ConnectionAttacher {
    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection);
  }

  private static final class DefaultConnectionAttacher implements ConnectionAttacher {

    private final MessageTransportBase transport;
    private final TCLogger             logger;

    private DefaultConnectionAttacher(MessageTransportBase transport, TCLogger logger) {
      this.transport = transport;
      this.logger = logger;
    }

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

  public void connectEvent(TCConnectionEvent event) {
    return;
  }

  public void closeEvent(TCConnectionEvent event) {
    boolean isSameConnection = false;

    synchronized (attachingNewConnection) {
      TCConnection src = event.getSource();
      isSameConnection = (src == this.connection);
      if (isSameConnection) {
        this.connectionCloseEvent.set(event);
      }
    }

    if (isSameConnection) {
      boolean forcedDisconnect = false;
      synchronized (status) {
        logger.warn("CLOSE EVENT : " + this.connection + ". STATUS : " + status);
        if (status.isEstablished() || status.isDisconnected()) {
          if (status.isDisconnected()) forcedDisconnect = true;
          status.reset();
        } else {
          logger.warn("closing down connection - " + event);
          return;
        }
      }

      if (forcedDisconnect) {
        fireTransportForcedDisconnectEvent();
      } else {
        fireTransportDisconnectedEvent();
      }
    }
  }

  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    return;
  }

  public void endOfFileEvent(TCConnectionEvent event) {
    return;
  }

  protected void handleHandshakeError(TransportHandshakeErrorContext e) {
    this.handshakeErrorHandler.handleHandshakeError(e);
  }

  protected TCConnection getConnection() {
    return connection;
  }

  public TCSocketAddress getRemoteAddress() {
    return (connection != null ? this.connection.getRemoteAddress() : null);
  }

  public TCSocketAddress getLocalAddress() {
    return (connection != null ? this.connection.getLocalAddress() : null);
  }

  protected void setConnection(TCConnection conn) {
    TCConnection old = this.connection;
    this.connection = conn;
    this.connection.addListener(this);
    if (old != null) {
      old.removeListener(this);
    }
  }

  protected void clearConnection() {
    TCConnection conn;
    if ((conn = getConnection()) != null) {
      conn.close(10000);
      this.connectionId = new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong());
      conn.removeListener(this);
      this.connection = null;
    }
  }

  protected void wireNewConnection(TCConnection conn) {
    logger.info("Attaching new connection: " + conn);

    synchronized (status) {
      if (this.status.isClosed()) {
        logger.warn("Connection stack is already closed. " + this.status + "; Conn: " + conn);
        conn.removeListener(this);
        conn.asynchClose();
      } else {
        setConnection(conn);
        this.status.reset();
      }
    }

  }

  /**
   * this function gets the stackLayerFlag added to build the communication stack information
   */
  public short getStackLayerFlag() {
    // this is the transport layer
    return TYPE_TRANSPORT_LAYER;
  }

  /**
   * This function gets the stack layer name of the present layer added to build the communication stack information
   */
  public String getStackLayerName() {
    // this is the transport layer
    return NAME_TRANSPORT_LAYER;
  }

  public synchronized int getRemoteCallbackPort() {
    return this.remoteCallbackPort;
  }

  public synchronized void setRemoteCallbackPort(int remoteCallbackPort) {
    this.remoteCallbackPort = remoteCallbackPort;
  }

  @Override
  public void initConnectionID(ConnectionID cid) {
    connectionId = cid;
  }

}
