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

import org.slf4j.LoggerFactory;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.ConnectionIdLogger;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.TCExceptionResultException;
import com.tc.util.concurrent.TCFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client implementation of the transport network layer.
 */
public class ClientMessageTransport extends MessageTransportBase {
  public static final long                  TRANSPORT_HANDSHAKE_SYNACK_TIMEOUT = TCPropertiesImpl
                                                                                   .getProperties()
                                                                                   .getLong(TCPropertiesConsts.TC_TRANSPORT_HANDSHAKE_TIMEOUT,
                                                                                            10000);
  private final TCConnectionManager connectionManager;
  private boolean                           wasOpened                          = false;
  private TCFuture                          waitForSynAckResult;
  private final WireProtocolAdaptorFactory  wireProtocolAdaptorFactory;
  private final AtomicBoolean               isOpening                          = new AtomicBoolean(false);
  private final int                         callbackPort;
  private final int                         timeout;

  public ClientMessageTransport(TCConnectionManager clientConnectionEstablisher,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory,
                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory, int callbackPort, int timeout) {
    this(clientConnectionEstablisher, handshakeErrorHandler, messageFactory, wireProtocolAdaptorFactory, callbackPort, timeout,
         ReconnectionRejectedHandlerL1.SINGLETON);
  }

  /**
   * Constructor for when you want a transport that isn't connected yet (e.g., in a client). This constructor will
   * create an unopened MessageTransport.
   */
  public ClientMessageTransport(TCConnectionManager connectionManager,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory,
                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory, int callbackPort, int timeout,
                                ReconnectionRejectedHandler reconnectionRejectedHandler) {

    super(MessageTransportState.STATE_START, handshakeErrorHandler, messageFactory, false, LoggerFactory.getLogger(ClientMessageTransport.class));
    this.wireProtocolAdaptorFactory = wireProtocolAdaptorFactory;
    this.connectionManager = connectionManager;
    this.callbackPort = callbackPort;
    this.timeout = timeout;
  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws MaxConnectionsExceededException
   */
  @Override
  public NetworkStackID open(ConnectionInfo info) throws TCTimeoutException, IOException, MaxConnectionsExceededException,
      CommStackMismatchException {
    // XXX: This extra boolean flag is dumb, but it's here because the close event can show up
    // while the lock on isOpen is held here. That will cause a deadlock because the close event is thrown on the
    // comms thread which means that the handshake messages can't be sent.
    // The state machine here needs to be rationalized.
    this.isOpening.set(true);
    synchronized (this.isOpen) {
      Assert.eval("can't open an already open transport", !this.isOpen.get());
      Assert.eval("can't open an already connected transport", !this.isConnected());

      TCSocketAddress socket = new TCSocketAddress(info);
      TCConnection connection = connect(socket);
      try {
        openConnection(connection);
      } catch (CommStackMismatchException e) {
        connection.close(100);
        throw e;
      } catch (MaxConnectionsExceededException e) {
        connection.close(100);
        throw e;
      } catch (TCTimeoutException e) {
        connection.close(100);
        throw e;
      } catch (TransportHandshakeException e) {
        connection.close(100);
        throw e;
      }
      Assert.eval(!getConnectionId().isNull());
      this.isOpen.set(true);
      NetworkStackID nid = new NetworkStackID(getConnectionId().getChannelID());
      this.wasOpened = true;
      this.isOpening.set(false);
      return (nid);
    }
  }
  /**
   * Tries to make a connection. This is a blocking call.
   * 
   * @return
   * @throws TCTimeoutException
   * @throws IOException
   * @throws MaxConnectionsExceededException
   */
  TCConnection connect(TCSocketAddress sa) throws TCTimeoutException, IOException {
    TCConnection connection = this.connectionManager.createConnection(getProtocolAdapter());
    fireTransportConnectAttemptEvent();
    try {
      connection.connect(sa, this.timeout);
    } catch (IOException e) {
      connection.close(100);
      throw e;
    } catch (TCTimeoutException e) {
      connection.close(100);
      throw e;
    }
    return connection;
  }
  
  @Override
  public void reset() {
    synchronized (this.isOpen) {
      getLogger().info("Resetting connection " + getConnectionId());
      this.disconnect();
      this.isOpen.set(false);
      this.status.reset();
      clearConnection();
    }
  }

  private void handleHandshakeError(HandshakeResult result) throws TransportHandshakeException, MaxConnectionsExceededException,
      CommStackMismatchException, ReconnectionRejectedException {
    if (result.hasErrorContext()) {
      switch (result.getError()) {
        case ERROR_NO_ACTIVE:
          if (this.getConnectionId().getProductId().isRedirectEnabled()) {
            throw new NoActiveException();
          }
          break;
        case ERROR_MAX_CONNECTION_EXCEED:
          cleanConnectionWithoutNotifyListeners();
          throw new MaxConnectionsExceededException(getMaxConnectionsExceededMessage(result.maxConnections()));
        case ERROR_STACK_MISMATCH:
          cleanConnectionWithoutNotifyListeners();
          throw new CommStackMismatchException("Disconnected due to comm stack mismatch");
        case ERROR_RECONNECTION_REJECTED:
          cleanConnectionWithoutNotifyListeners();
          fireTransportReconnectionRejectedEvent();
          throw new ReconnectionRejectedException(
                                                  "Reconnection rejected by L2 due to stack not found. Client will be unable to join the cluster again unless rejoin is enabled.");
        case ERROR_REDIRECT_CONNECTION:
          if (this.getConnectionId().getProductId().isRedirectEnabled()) {
            throw new TransportRedirect(result.synAck.getErrorContext());
          }
          break;
        case ERROR_PRODUCT_NOT_SUPPORTED:
        default:
          throw new TransportHandshakeException("Disconnected due to transport handshake error: " + result.getError());
      }
    }
  }

  /*
   * Do not trigger reconnection
   */
  private void cleanConnectionWithoutNotifyListeners() {
    List<MessageTransportListener> tl = new ArrayList<MessageTransportListener>(this.getTransportListeners());
    this.removeTransportListeners();
    clearConnection();
    this.addTransportListeners(tl);
    this.status.reset();
  }

  /**
   * Returns true if the MessageTransport was ever in an open state.
   */
  public boolean wasOpened() {
    synchronized (isOpen) {
      return this.wasOpened;
    }
  }

  public boolean isNotOpen() {
    return !this.isOpening.get() && !this.isOpen.get();
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    if (isNotOpen()) { return; }
    super.closeEvent(event);
    setSynAckResult(new IOException("connection closed"));
  }

  @Override
  protected void receiveTransportMessageImpl(WireProtocolMessage message) {
    synchronized (this.status) {
      if (this.status.isSynSent()) {
        handleSynAck(message);
        message.recycle();
        return;
      }

      if (!this.status.isEstablished()) {
        this.getLogger().debug("Ignoring the message received for an Un-Established Connection; " + message.getSource()
                         + "; " + message);
        message.recycle();
        return;
      }
    }
    super.receiveToReceiveLayer(message);
  }

  private void handleSynAck(WireProtocolMessage message) {
    if (!verifySynAck(message)) {
      handleHandshakeError(new TransportHandshakeErrorContext(
                                                              "Received a message that was not a SYN_ACK while waiting for SYN_ACK: "
                                                                  + message, TransportHandshakeError.ERROR_HANDSHAKE));
    } else {
      SynAckMessage synAck = (SynAckMessage) message;
      if (synAck.hasErrorContext()) {
        if (synAck.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) {
          handleHandshakeError(new TransportHandshakeErrorContext(getCommsStackMismatchErrorMessage(synAck)
                                                                  + "\n\nPLEASE RECONFIGURE THE STACKS",
                                                                  synAck.getErrorType()));
        } else {
          handleHandshakeError(new TransportHandshakeErrorContext(synAck.getErrorContext() + message,
                                                                  synAck.getErrorType()));
        }
      }

      if (!getConnectionId().isNewConnection() && getConnectionId().isValid()) {
        // This is a reconnect
        Assert.eval(!synAck.getConnectionId().isValid() || getConnectionId().equals(synAck.getConnectionId()));
      }
      getConnection().setTransportEstablished();
      setSynAckResult(synAck);
      setRemoteCallbackPort(synAck.getCallbackPort());
    }
  }
  
  private void setSynAckResult(Object msg) {
    synchronized (status) {
      if (this.waitForSynAckResult != null) {
        if (msg instanceof Exception) {
          this.waitForSynAckResult.setException((Exception)msg);
        } else {
          this.waitForSynAckResult.set(msg);
        }
        this.waitForSynAckResult = null;
      }
    }
  }

  /**
   * If communication stacks are mismatched then get the client side communication stack and append in the error message
   */
  private String getCommsStackMismatchErrorMessage(SynAckMessage synAck) {
    String errorMessage = "\n\nLayers Present in Client side communication stack: ";
    // get the names of stack layers present
    errorMessage += getCommunicationStackNames(this);
    errorMessage = "\nTHERE IS A MISMATCH IN THE COMMUNICATION STACKS\n" + synAck.getErrorContext() + errorMessage;
    if ((getCommunicationStackFlags(this) & NetworkLayer.TYPE_OOO_LAYER) != 0) {
      this.getLogger().error(NetworkLayer.ERROR_OOO_IN_CLIENT_NOT_IN_SERVER);
      errorMessage = "\n\n" + NetworkLayer.ERROR_OOO_IN_CLIENT_NOT_IN_SERVER + errorMessage;
    } else {
      this.getLogger().error(NetworkLayer.ERROR_OOO_IN_SERVER_NOT_IN_CLIENT);
      errorMessage = "\n\n" + NetworkLayer.ERROR_OOO_IN_SERVER_NOT_IN_CLIENT + errorMessage;
    }
    return errorMessage;
  }

  private boolean verifySynAck(TCNetworkMessage message) {
    // XXX: yuck.
    return message instanceof TransportHandshakeMessage && ((TransportHandshakeMessage) message).isSynAck();
  }

  /**
   * Builds a protocol stack and tries to make a connection. This is a blocking call.
   * 
   * @throws TCTimeoutException
   */
  HandshakeResult handShake() throws TCTimeoutException, TransportHandshakeException {
    try {
      SynAckMessage synAck = (SynAckMessage)sendSyn().get(TRANSPORT_HANDSHAKE_SYNACK_TIMEOUT);
      return new HandshakeResult(synAck);
    } catch (InterruptedException e) {
      throw new TransportHandshakeException(e);
    } catch (TCExceptionResultException e) {
      throw new TransportHandshakeException("Client was able to establish connection with server but handshake " +
          "with server failed.", e);
    }
  }

  private TCFuture sendSyn() {
    TCFuture targetFuture = new TCFuture(this.status);
    getConnection().addWeight(MessageTransport.CONNWEIGHT_TX_HANDSHAKED);
    synchronized (this.status) {
      if (this.status.isEstablished() || this.status.isSynSent()) { throw new AssertionError(" ERROR !!! "
                                                                                             + this.status); }
      this.waitForSynAckResult = targetFuture;
      // get the stack layer list and pass it in
      short stackLayerFlags = getCommunicationStackFlags(this);
      TransportHandshakeMessage syn = this.messageFactory.createSyn(getConnectionId(), getConnection(),
                                                                    stackLayerFlags, this.callbackPort);
      // send syn message
      try {
        this.sendToConnection(syn);
        this.status.synSent();
      } catch (IOException ioe) {
        logger.warn("trouble syn", ioe);
      }
    }
    
    return targetFuture;
  }

  private void sendAck() throws TransportHandshakeException {
    synchronized (this.status) {
      // DEV-1364 : Connection close might have happened
      if (!this.status.isSynSent()) {
        throw new TransportHandshakeException("Transport is not " + MessageTransportState.STATE_SYN_SENT
                                              + ". Status: " + status);
      }
      TransportHandshakeMessage ack = this.messageFactory.createAck(getConnectionId(), getConnection());
      // send ack message
      try {
        this.sendToConnection(ack);
      } catch (IOException ioe) {
        throw new TransportHandshakeException(ioe);
      }
      this.status.established();
    }
    fireTransportConnectedEvent();
  }

  protected void openConnection(TCConnection connection) throws TCTimeoutException, TransportHandshakeException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Assert.eval(!isConnected());
    if (wireNewConnection(connection)) {
      try {
        handshakeConnection();
      } catch (TCTimeoutException e) {
        clearConnection();
        this.status.reset();
        throw e;
      } catch (ReconnectionRejectedException e) {
        throw new TCRuntimeException("Should not happen here: " + e);
      } catch (TransportHandshakeException e) {
        clearConnection();
        this.status.reset();
        throw e;
      }
    } else {
      throw new TransportHandshakeException("connection closed");
    }
  }

  void reopen(ConnectionInfo info) throws Exception {

    // don't do reconnect if open is still going on
    if (!wasOpened()) {
      this.getLogger().info("Transport was opened already. Skip reconnect " + info);
      return;
    }
    
    TCSocketAddress socket = new TCSocketAddress(info);
    reconnect(socket);
  }
  
  void reconnect(TCSocketAddress socket) throws Exception {
    TCConnection connection = connect(socket);
      
    Assert.eval(!isConnected());
    if (wireNewConnection(connection)) {
      try {
        handshakeConnection();
      } catch (Exception t) {
        connection.close(100);
        this.status.reset();
        throw t;
      }
    }
  }

  private void handshakeConnection() throws TCTimeoutException, MaxConnectionsExceededException,
      TransportHandshakeException, CommStackMismatchException, ReconnectionRejectedException {
    HandshakeResult result = handShake();
    handleHandshakeError(result);
    initConnectionID(result.synAck.getConnectionId());
    sendAck();
    getConnectionId().authenticated();
    log("Handshake is complete");
  }

  private String getMaxConnectionsExceededMessage(int maxConnections) {
    return "Your product key only allows maximum " + maxConnections + " clients to connect.";
  }

  TCProtocolAdaptor getProtocolAdapter() {
    return this.wireProtocolAdaptorFactory.newWireProtocolAdaptor(new WireProtocolMessageSink() {
      @Override
      public void putMessage(WireProtocolMessage message) {
        receiveTransportMessage(message);
      }
    });
  }

  private static final class HandshakeResult {
    private final SynAckMessage synAck;

    private HandshakeResult(SynAckMessage synAck) {
      this.synAck = synAck;
    }

    public int maxConnections() {
      return this.synAck.getMaxConnections();
    }

    public boolean hasErrorContext() {
      return this.synAck.isMaxConnectionsExceeded() || this.synAck.hasErrorContext();
    }
    
    public boolean isConnectionValid() {
      return synAck.getConnectionId().isValid();
    }

    public TransportHandshakeError getError() {
      if (this.synAck.isMaxConnectionsExceeded()) {
        return TransportHandshakeError.ERROR_MAX_CONNECTION_EXCEED;
      } else {
        return this.synAck.getErrorType();
      }
    }

  }

  @Override
  protected void fireTransportConnectAttemptEvent() {
    super.fireTransportConnectAttemptEvent();
  }

  @Override
  public boolean isConnected() {
    return super.isConnected();
  }

  // method used for testing
  public void switchLoggerForTesting(ConnectionIdLogger tmpLogger) {
    this.logger = tmpLogger;
  }

  @Override
  public void sendToConnection(TCNetworkMessage message) throws IOException {
    // override just here to satisfy mocking in tests...
    super.sendToConnection(message); 
  }
  
  

}
