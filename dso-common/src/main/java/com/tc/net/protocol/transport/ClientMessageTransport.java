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
package com.tc.net.protocol.transport;

import com.tc.exception.TCInternalError;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.ConnectionIdLogger;
import com.tc.logging.TCLogging;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.ReconnectionRejectedException;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.ChannelID;
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
  private final ClientConnectionEstablisher connectionEstablisher;
  private boolean                           wasOpened                          = false;
  private TCFuture                          waitForSynAckResult;
  private final WireProtocolAdaptorFactory  wireProtocolAdaptorFactory;
  private final AtomicBoolean               isOpening                          = new AtomicBoolean(false);
  private final int                         callbackPort;
  private final TCSecurityManager           securityManager;
  private final ConnectionAddressProvider   addressProvider;

  public ClientMessageTransport(ClientConnectionEstablisher clientConnectionEstablisher,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory,
                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory, int callbackPort) {
    this(clientConnectionEstablisher, handshakeErrorHandler, messageFactory, wireProtocolAdaptorFactory, callbackPort,
         ReconnectionRejectedHandlerL1.SINGLETON, null, null);
  }

  /**
   * Constructor for when you want a transport that isn't connected yet (e.g., in a client). This constructor will
   * create an unopened MessageTransport.
   * 
   * @param securityManager
   * @param addressProvider
   */
  public ClientMessageTransport(ClientConnectionEstablisher clientConnectionEstablisher,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory,
                                WireProtocolAdaptorFactory wireProtocolAdaptorFactory, int callbackPort,
                                ReconnectionRejectedHandler reconnectionRejectedHandler,
                                final TCSecurityManager securityManager, final ConnectionAddressProvider addressProvider) {

    super(MessageTransportState.STATE_START, handshakeErrorHandler, messageFactory, false, TCLogging
        .getLogger(ClientMessageTransport.class));
    this.addressProvider = addressProvider;
    this.wireProtocolAdaptorFactory = wireProtocolAdaptorFactory;
    this.connectionEstablisher = clientConnectionEstablisher;
    this.callbackPort = callbackPort;
    this.securityManager = securityManager;
  }

  /**
   * Blocking open. Causes a connection to be made. Will throw exceptions if the connect fails.
   * 
   * @throws TCTimeoutException
   * @throws IOException
   * @throws MaxConnectionsExceededException
   */
  @Override
  public NetworkStackID open() throws TCTimeoutException, IOException, MaxConnectionsExceededException,
      CommStackMismatchException {
    // XXX: This extra boolean flag is dumb, but it's here because the close event can show up
    // while the lock on isOpen is held here. That will cause a deadlock because the close event is thrown on the
    // comms thread which means that the handshake messages can't be sent.
    // The state machine here needs to be rationalized.
    this.isOpening.set(true);
    synchronized (this.isOpen) {
      Assert.eval("can't open an already open transport", !this.isOpen.get());
      this.connectionEstablisher.open(this);
      Assert.eval(!this.connectionId.isNull());
      this.isOpen.set(true);
      NetworkStackID nid = new NetworkStackID(this.connectionId.getChannelID());
      this.wasOpened = true;
      this.isOpening.set(false);
      return (nid);
    }
  }

  @Override
  public void reset() {
    synchronized (this.isOpen) {
      getLogger().info("Resetting connection " + connectionId);
      this.isOpen.set(false);
      this.connectionEstablisher.reset();
      this.connectionId = new ConnectionID(JvmIDUtil.getJvmID(), ChannelID.NULL_ID.toLong());
    }
  }

  private void handleHandshakeError(HandshakeResult result) throws TransportHandshakeException, MaxConnectionsExceededException,
      CommStackMismatchException, ReconnectionRejectedException {
    if (result.hasErrorContext()) {
      switch (result.getErrorType()) {
        case TransportHandshakeError.ERROR_MAX_CONNECTION_EXCEED:
          cleanConnectionWithoutNotifyListeners();
          throw new MaxConnectionsExceededException(getMaxConnectionsExceededMessage(result.maxConnections()));
        case TransportHandshakeError.ERROR_STACK_MISMATCH:
          cleanConnectionWithoutNotifyListeners();
          throw new CommStackMismatchException("Disconnected due to comm stack mismatch");
        case TransportHandshakeError.ERROR_RECONNECTION_REJECTED:
          cleanConnectionWithoutNotifyListeners();
          fireTransportReconnectionRejectedEvent();
          throw new ReconnectionRejectedException(
                                                  "Reconnection rejected by L2 due to stack not found. Client will be unable to join the cluster again unless rejoin is enabled.");
        default:
          throw new TransportHandshakeException("Disconnected due to transport handshake error");
      }
    }
  }

  /*
   * Do not trigger reconnection
   */
  private void cleanConnectionWithoutNotifyListeners() {
    List tl = new ArrayList(this.getTransportListeners());
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

  // TODO :: come back
  @Override
  public void closeEvent(TCConnectionEvent event) {
    if (isNotOpen()) { return; }
    super.closeEvent(event);
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
        this.getLogger().warn("Ignoring the message received for an Un-Established Connection; " + message.getSource()
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

      if (!this.connectionId.isNewConnection()) {
        // This is a reconnect
        Assert.eval(this.connectionId.equals(synAck.getConnectionId()));
      }
      if (!synAck.isMaxConnectionsExceeded()) {
        this.connectionId = synAck.getConnectionId();
        Assert.assertNotNull("Connection id from the server was null!", this.connectionId);
        Assert.eval(!ConnectionID.NULL_ID.equals(this.connectionId));
        Assert.assertNotNull(this.waitForSynAckResult);
      }
      getConnection().setTransportEstablished();
      this.waitForSynAckResult.set(synAck);
      setRemoteCallbackPort(synAck.getCallbackPort());
    }
  }

  /**
   * If communication stacks are mismatched then get the client side communication stack and append in the error message
   */
  private String getCommsStackMismatchErrorMessage(final SynAckMessage synAck) {
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
  HandshakeResult handShake() throws TCTimeoutException {
    sendSyn();
    SynAckMessage synAck = waitForSynAck();
    return new HandshakeResult(synAck);
  }

  private SynAckMessage waitForSynAck() throws TCTimeoutException {
    try {
      SynAckMessage synAck = (SynAckMessage) this.waitForSynAckResult.get(TRANSPORT_HANDSHAKE_SYNACK_TIMEOUT);
      return synAck;
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    } catch (TCExceptionResultException e) {
      throw new TCInternalError(e);
    }
  }

  private void sendSyn() {
    getConnection().addWeight(MessageTransport.CONNWEIGHT_TX_HANDSHAKED);
    synchronized (this.status) {
      if (this.status.isEstablished() || this.status.isSynSent()) { throw new AssertionError(" ERROR !!! "
                                                                                             + this.status); }
      this.waitForSynAckResult = new TCFuture(this.status);
      // get the stack layer list and pass it in
      short stackLayerFlags = getCommunicationStackFlags(this);
      if (connectionId.isSecured() && connectionId.getPassword() == null) {
        // Re-init the password
        ConnectionInfo connectionInfo = addressProvider.getIterator().next();
        connectionId.setPassword(securityManager.getPasswordForTC(connectionId.getUsername(),
                                                                  connectionInfo.getHostname(),
                                                                  connectionInfo.getPort()));
      }
      TransportHandshakeMessage syn = this.messageFactory.createSyn(this.connectionId, getConnection(),
                                                                    stackLayerFlags, this.callbackPort);
      // send syn message
      this.sendToConnection(syn);
      this.status.synSent();
    }
  }

  private void sendAck() throws TransportHandshakeException {
    synchronized (this.status) {
      // DEV-1364 : Connection close might have happened
      if (!this.status.isSynSent()) {
        throw new TransportHandshakeException("Transport is not " + MessageTransportState.STATE_SYN_SENT
                                              + ". Status: " + status);
      }
      TransportHandshakeMessage ack = this.messageFactory.createAck(this.connectionId, getConnection());
      // send ack message
      this.sendToConnection(ack);
      this.status.established();
    }
    fireTransportConnectedEvent();
  }

  protected void openConnection(TCConnection connection) throws TCTimeoutException, TransportHandshakeException,
      MaxConnectionsExceededException, CommStackMismatchException {
    Assert.eval(!isConnected());
    wireNewConnection(connection);
    try {
      handshakeConnection(connection);
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
  }

  void reconnect(TCConnection connection) throws Exception {

    // don't do reconnect if open is still going on
    if (!wasOpened()) {
      this.getLogger().warn("Transport was opened already. Skip reconnect " + connection);
      return;
    }

    Assert.eval(!isConnected());
    wireNewConnection(connection);
    try {
      handshakeConnection(connection);
    } catch (Exception t) {
      this.status.reset();
      throw t;
    }
  }

  private void handshakeConnection(TCConnection connection) throws TCTimeoutException, MaxConnectionsExceededException,
      TransportHandshakeException, CommStackMismatchException, ReconnectionRejectedException {
    HandshakeResult result = handShake();
    handleHandshakeError(result);
    sendAck();
    connectionId.authenticated();
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

    public short getErrorType() {
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

  public ClientConnectionEstablisher getConnectionEstablisher() {
    return this.connectionEstablisher;
  }

  // method used for testing
  public void switchLoggerForTesting(ConnectionIdLogger tmpLogger) {
    this.logger = tmpLogger;
  }

}
