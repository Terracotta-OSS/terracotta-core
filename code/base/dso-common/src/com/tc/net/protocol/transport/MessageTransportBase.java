/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.bytes.TCByteBuffer;
import com.tc.logging.ConnectionIDProvider;
import com.tc.logging.TCLogger;
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
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;

/**
 * Implementation of MessaageTransport
 */
abstract class MessageTransportBase extends AbstractMessageTransport implements NetworkLayer,
    TCConnectionEventListener, ConnectionIDProvider {
  private TCConnection                             connection;

  protected ConnectionID                           connectionId           = ConnectionID.NULL_ID;
  protected final MessageTransportStatus           status;
  protected final SynchronizedBoolean              isOpen;
  protected final TransportHandshakeMessageFactory messageFactory;
  private final TransportHandshakeErrorHandler     handshakeErrorHandler;
  private NetworkLayer                             receiveLayer;

  private final Object                             attachingNewConnection = new Object();
  private final SynchronizedRef                    connectionCloseEvent   = new SynchronizedRef(null);
  private byte[]                                   sourceAddress;
  private int                                      sourcePort;
  private byte[]                                   destinationAddress;
  private int                                      destinationPort;
  private boolean                                  allowConnectionReplace = false;
  private ConnectionHealthCheckerContext           healthCheckerContext   = null;
  private int                                      remoteCallbackPort     = TransportHandshakeMessage.NO_CALLBACK_PORT;

  protected MessageTransportBase(MessageTransportState initialState,
                                 TransportHandshakeErrorHandler handshakeErrorHandler,
                                 TransportHandshakeMessageFactory messageFactory, boolean isOpen, TCLogger logger) {

    super(logger);
    this.handshakeErrorHandler = handshakeErrorHandler;
    this.messageFactory = messageFactory;
    this.isOpen = new SynchronizedBoolean(isOpen);
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
        logger.warn("Received message from an old connection: " + message);
      }
    }
  }

  public abstract NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, IOException;

  protected abstract void receiveTransportMessageImpl(WireProtocolMessage message);

  protected final void receiveToReceiveLayer(WireProtocolMessage message) {
    Assert.assertNotNull(receiveLayer);
    if (message.getMessageProtocol() == WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE) {
      // message is printed for debugging
      logger.info(message.toString());
      throw new AssertionError("Wrong handshake message from: " + message.getSource());
    } else if (message.getMessageProtocol() == WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES) {
      Assert.assertNotNull(healthCheckerContext);
      if (this.healthCheckerContext.receiveProbe((HealthCheckerProbeMessage) message)) {
        // Proper health checker probe message. The context would have taken care of processing.
        return;
      }
      throw new AssertionError("Wrong HealthChecker Probe message from: " + message.getSource());
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
        if (!this.status.isEnd()) this.status.disconnect();
        // Dont fire any events here. Anyway asynchClose is triggered below and we are expected to receive a closeEvent
        // and upon which we open up the OOO Reconnect window
      } else {
        if (!this.status.isEnd()) this.status.closed();
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

  public final void sendToConnection(TCNetworkMessage message) {
    if (message == null) throw new AssertionError("Attempt to send a null message.");
    if (!(message instanceof WireProtocolMessage)) {
      final TCNetworkMessage payload = message;

      message = WireProtocolMessageImpl.wrapMessage(message, connection);
      Assert.eval(message.getSentCallback() == null);

      final Runnable callback = payload.getSentCallback();
      if (callback != null) {
        message.setSentCallback(new Runnable() {
          public void run() {
            callback.run();
          }
        });
      }
    }

    WireProtocolHeader hdr = (WireProtocolHeader) message.getHeader();

    hdr.setSourceAddress(getSourceAddress());
    hdr.setSourcePort(getSourcePort());
    hdr.setDestinationAddress(getDestinationAddress());
    hdr.setDestinationPort(getDestinationPort());
    hdr.computeChecksum();

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
    return new DefaultConnectionAttacher(this);
  }

  protected interface ConnectionAttacher {
    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection);
  }

  private static final class DefaultConnectionAttacher implements ConnectionAttacher {

    private final MessageTransportBase transport;

    private DefaultConnectionAttacher(MessageTransportBase transport) {
      this.transport = transport;
    }

    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection) {
      Assert.assertNotNull(oldConnection);
      if (closeEvent == null || closeEvent.getSource() != oldConnection) {
        // We either didn't receive a close event or we received a close event
        // from a connection that isn't our current connection.
        this.transport.fireTransportDisconnectedEvent();
      }
      // remove the transport as a listener for the old connection
      if (oldConnection != null && oldConnection != transport.connection) {
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
      fireTransportDisconnectedEvent();
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

  protected void handleHandshakeError(TransportHandshakeErrorContext e, TransportHandshakeMessage m) {
    this.handshakeErrorHandler.handleHandshakeError(e, m);
  }

  protected TCConnection getConnection() {
    return connection;
  }

  public TCSocketAddress getRemoteAddress() {
    return this.connection.getRemoteAddress();
  }

  public TCSocketAddress getLocalAddress() {
    return this.connection.getLocalAddress();
  }

  protected void setConnection(TCConnection conn) {
    TCConnection old = this.connection;
    this.connection = conn;
    clearAddressCache();
    this.connection.addListener(this);
    if (old != null) {
      old.removeListener(this);
    }
  }

  protected void clearConnection() {
    TCConnection conn;
    if ((conn = getConnection()) != null) {
      conn.close(10000);
      this.connectionId = ConnectionID.NULL_ID;
      conn.removeListener(this);
      clearAddressCache();
      this.connection = null;
    }
  }

  private void clearAddressCache() {
    this.sourceAddress = null;
    this.sourcePort = -1;
    this.destinationAddress = null;
    this.destinationPort = -1;
  }

  private byte[] getSourceAddress() {
    if (sourceAddress == null) { return sourceAddress = connection.getLocalAddress().getAddressBytes(); }
    return sourceAddress;
  }

  private byte[] getDestinationAddress() {
    if (destinationAddress == null) { return destinationAddress = connection.getRemoteAddress().getAddressBytes(); }
    return destinationAddress;
  }

  private int getSourcePort() {
    if (sourcePort == -1) { return this.sourcePort = connection.getLocalAddress().getPort(); }
    return sourcePort;
  }

  private int getDestinationPort() {
    if (destinationPort == -1) { return this.destinationPort = connection.getRemoteAddress().getPort(); }
    return sourcePort;
  }

  protected void wireNewConnection(TCConnection conn) {
    logger.info("Attaching new connection: " + conn);
    setConnection(conn);
    this.status.reset();
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

}
