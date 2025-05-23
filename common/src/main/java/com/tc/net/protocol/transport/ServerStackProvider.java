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
package com.tc.net.protocol.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.net.ClientID;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.ProductNotSupportedException;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.RejectReconnectionException;
import com.tc.net.protocol.ServerNetworkStackHarness;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.tcm.msgs.CommsMessageFactory;
import com.tc.util.Assert;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.RedirectAddressProvider;
import java.net.InetSocketAddress;

/**
 * Provides network stacks on the server side
 */
public class ServerStackProvider implements NetworkStackProvider, MessageTransportListener, ProtocolAdaptorFactory {
  private static final Logger logger = LoggerFactory.getLogger(ServerStackProvider.class);

  private final Map<ClientID, ServerNetworkStackHarness> harnesses          = new ConcurrentHashMap<>();
  private final NetworkStackHarnessFactory       harnessFactory;
  private final ServerMessageChannelFactory      channelFactory;
  private final TransportHandshakeMessageFactory handshakeMessageFactory;
  private final ConnectionIDFactory              connectionIdFactory;
  private final ConnectionPolicy                 connectionPolicy;
  private final WireProtocolAdaptorFactory       wireProtocolAdaptorFactory;
  private final WireProtocolMessageSink          wireProtoMsgsink;
  private final MessageTransportFactory          messageTransportFactory;
  private final List<MessageTransportListener>   transportListeners = new ArrayList<>();
  private final ReentrantLock                    licenseLock;
  private final RedirectAddressProvider                 activeProvider;
  private final Predicate<MessageTransport>                validateTransport;

  // used only in test
  public ServerStackProvider(Set<ConnectionID> initialConnectionIDs, NetworkStackHarnessFactory harnessFactory,
                             ServerMessageChannelFactory channelFactory,
                             MessageTransportFactory messageTransportFactory,
                             TransportHandshakeMessageFactory handshakeMessageFactory,
                             ConnectionIDFactory connectionIdFactory, ConnectionPolicy connectionPolicy,
                             WireProtocolAdaptorFactory wireProtocolAdaptorFactory, ReentrantLock licenseLock) {
    this(initialConnectionIDs, null, (t)->true, harnessFactory, channelFactory, messageTransportFactory,
         handshakeMessageFactory, connectionIdFactory, connectionPolicy, wireProtocolAdaptorFactory, null, licenseLock);
  }

  public ServerStackProvider(Set<ConnectionID> initialConnectionIDs, RedirectAddressProvider activeProvider, Predicate<MessageTransport> validate, NetworkStackHarnessFactory harnessFactory,
                             ServerMessageChannelFactory channelFactory,
                             MessageTransportFactory messageTransportFactory,
                             TransportHandshakeMessageFactory handshakeMessageFactory,
                             ConnectionIDFactory connectionIdFactory, ConnectionPolicy connectionPolicy,
                             WireProtocolAdaptorFactory wireProtocolAdaptorFactory,
                             WireProtocolMessageSink wireProtoMsgSink, ReentrantLock licenseLock) {
    this.messageTransportFactory = messageTransportFactory;
    this.connectionPolicy = connectionPolicy;
    this.wireProtocolAdaptorFactory = wireProtocolAdaptorFactory;
    this.wireProtoMsgsink = wireProtoMsgSink;
    Assert.assertNotNull(harnessFactory);
    this.harnessFactory = harnessFactory;
    this.channelFactory = channelFactory;
    this.handshakeMessageFactory = handshakeMessageFactory;
    this.connectionIdFactory = connectionIdFactory;
    this.transportListeners.add(this);
    Assert.assertNotNull(licenseLock);
    this.licenseLock = licenseLock;
    for (final ConnectionID client : initialConnectionIDs) {
      logger.info("Preparing comms stack for previously connected client: " + client);
      MessageTransport transport = messageTransportFactory.createNewTransport(
          createHandshakeErrorHandler(),
          handshakeMessageFactory,
          transportListeners);
      //  create a fake connection id for use until the real transport connection is made
      transport.initConnectionID(client);
      newStackHarness(client.getClientID(), transport).finalizeStack();
    }
    this.activeProvider = activeProvider;
    this.validateTransport = validate;
  }

  @Override
  public MessageTransport attachNewConnection(ConnectionID connectionId, TCConnection connection)
      throws RejectReconnectionException, ProductNotSupportedException {
    Assert.assertNotNull(connection);

    ServerNetworkStackHarness harness;
    final MessageTransport rv;
    if (this.activeProvider != null || connectionId.isNewConnection()) {
      connectionId = connectionIdFactory.populateConnectionID(connectionId);
      
      if (connectionId == ConnectionID.NULL_ID) {
        throw new ProductNotSupportedException(connectionId.getProductId() + " not supported");
      }

      rv = messageTransportFactory.createNewTransport(connection, createHandshakeErrorHandler(),
          handshakeMessageFactory, transportListeners);
      rv.initConnectionID(connectionId);
      newStackHarness(connectionId.getClientID(), rv).finalizeStack();
    } else {
      harness = harnesses.get(connectionId.getClientID());
      if (harness == null) {
        throw new RejectReconnectionException("Stack for " + connectionId +" not found.", connection.getRemoteAddress());
      } else {
        connectionId = connectionIdFactory.populateConnectionID(connectionId);
        //  if a null connection id is returned, this means something is wrong with the
        //  connection ID and it cannot be reconnected.  Either the product is no longer
        //  supported or the servers do not match
        if (connectionId == ConnectionID.NULL_ID) {
          throw new RejectReconnectionException("Stack for " + connectionId +" not found.", connection.getRemoteAddress());
        }
        try {
          harness.getTransport().initConnectionID(connectionId);
          rv = harness.attachNewConnection(connection);
        } catch (IllegalReconnectException e) {
          logger.warn("Client attempting an illegal reconnect for id " + connectionId + ", " + connection);
          throw new RejectReconnectionException("Illegal reconnect attempt from " + connectionId + ".", connection.getRemoteAddress());
        }
      }
    }

    return rv;
  }

  private NetworkStackHarness newStackHarness(ClientID id, MessageTransport transport) {
    final ServerNetworkStackHarness harness;
    harness = harnessFactory.createServerHarness(channelFactory, transport, new MessageTransportListener[] { this });
    Object previous = harnesses.put(id, harness);
    if (previous != null) { throw new AssertionError("previous is " + previous + "connectionID:" + id + "new is"
                                                     + harness); }
    return harness;
  }

  private TransportHandshakeErrorHandler createHandshakeErrorHandler() {
    return new TransportHandshakeErrorHandler() {
      @Override
      public void handleHandshakeError(TransportHandshakeErrorContext thec) {
        logger.info(thec.getMessage());
      }
    };
  }

  /*********************************************************************************************************************
   * MessageTransportListener methods.
   */
  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    // don't care
  }

  /**
   * A client disconnected.
   */
  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    /*
     * Even for temporary disconnects, we need to keep proper accounting. Otherwise, the same client reconnect may fail.
     */
    this.connectionPolicy.clientDisconnected(transport.getConnectionID());
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    // don't care
  }

  /**
   * The connection was closed. The client is never allowed to reconnect. Removes stack associated with the given
   * transport from the map of managed stacks.
   */
  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    ConnectionID connectionId = transport.getConnectionID();
    ClientID cid = connectionId.getClientID();
    ServerNetworkStackHarness harness = harnesses.get(cid);
    if (harness != null && harness.getChannel().isClosed()) {
      harnesses.remove(cid);
      // should have been removed by the disconnect, leave this call here
      // in case that is not the case, evaluate assertion or removal at a later time
      if (!connectionId.isJvmIDNull()) {
        boolean removed = this.connectionPolicy.clientDisconnected(connectionId);
        if (removed) {
          logger.warn("connectionid not removed by transport disconnect");
        }
      }
    }
  }

  @Override
  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

  /*********************************************************************************************************************
   * ProtocolAdaptorFactory interface
   */

  @Override
  public TCProtocolAdaptor getInstance() {
    if (wireProtoMsgsink != null) {
      return this.wireProtocolAdaptorFactory.newWireProtocolAdaptor(wireProtoMsgsink);
    } else {
      MessageSink sink = new MessageSink(createHandshakeErrorHandler());
      return this.wireProtocolAdaptorFactory.newWireProtocolAdaptor(sink);
    }
  }

  /*********************************************************************************************************************
   * private stuff
   */

  class MessageSink implements WireProtocolMessageSink {
    private final TransportHandshakeErrorHandler handshakeErrorHandler;
    private volatile boolean                     isSynReceived    = false;
    private volatile boolean                     isHandshakeError = false;
    private volatile MessageTransport            transport;

    private MessageSink(TransportHandshakeErrorHandler handshakeErrorHandler) {
      this.handshakeErrorHandler = handshakeErrorHandler;
    }

    @Override
    public void putMessage(WireProtocolMessage message) {
      if (!isSynReceived) {
        synchronized (this) {
          if (!isSynReceived) {
            isSynReceived = verifyAndHandleSyn(message);
            return;
          }
        }
      }
      if (!isHandshakeError) {
        this.transport.receiveTransportMessage(message);
      } else {
        throw new AssertionError("clients should not send messages after handshake error");
      }
    }

    private boolean verifyAndHandleSyn(WireProtocolMessage message) {

      boolean isSynced = false;
      if (!verifySyn(message)) {
        handleHandshakeError(new TransportHandshakeErrorContext("Expected a SYN message but received: " + message,
                                                                TransportHandshakeError.ERROR_HANDSHAKE));
      } else {
        try {
          handleSyn((SynMessage) message);
          isSynced = true;
        } catch (RejectReconnectionException e) {
          // clients don't have valid callback ports
          String errorMessage = CommsMessageFactory.createReconnectRejectMessage(((SynMessage)message).getCallbackPort() > 0,
                                                                                 new Object[] { e.getMessage() });
          // send connection rejected SycAck back to L1
          // since stack no found, create a temp transport for sending sycAck message back
          this.transport = messageTransportFactory.createNewTransport(((SynMessage) message).getSource(),
                                                                      createHandshakeErrorHandler(),
                                                                      handshakeMessageFactory, transportListeners);
          this.transport.initConnectionID(((SynMessage) message).getConnectionId());
          TransportHandshakeErrorContext cxt = new TransportHandshakeErrorContext(errorMessage,
                                                        TransportHandshakeError.ERROR_RECONNECTION_REJECTED);
          sendSynAck(((SynMessage) message).getConnectionId(), cxt, ((SynMessage) message).getSource(), false);

          handleHandshakeError(new TransportHandshakeErrorContext(errorMessage, e));
        } catch (ProductNotSupportedException product) {
          // send connection rejected SycAck back to L1
          // since stack no found, create a temp transport for sending sycAck message back
          this.transport = messageTransportFactory.createNewTransport(((SynMessage) message).getSource(),
                                                                      createHandshakeErrorHandler(),
                                                                      handshakeMessageFactory, transportListeners);
          this.transport.initConnectionID(((SynMessage) message).getConnectionId());
          TransportHandshakeErrorContext cxt = new TransportHandshakeErrorContext(product.getMessage(),
                                                        TransportHandshakeError.ERROR_PRODUCT_NOT_SUPPORTED);
          sendSynAck(((SynMessage) message).getConnectionId(), cxt,
                     ((SynMessage) message).getSource(), false);

          handleHandshakeError(cxt);
        }
      }
      return isSynced;
    }

    private void handleHandshakeError(TransportHandshakeErrorContext ctxt) {
      this.isHandshakeError = true;
      this.handshakeErrorHandler.handleHandshakeError(ctxt);
    }

    private void handleSyn(SynMessage syn) throws RejectReconnectionException, ProductNotSupportedException {
      ConnectionID connectionId = syn.getConnectionId();
      boolean isMaxConnectionReached = false;

      if (connectionId == null) {
        this.transport = messageTransportFactory.createNewTransport(syn.getSource(),
            createHandshakeErrorHandler(),
            handshakeMessageFactory, transportListeners);
        sendSynAck(new TransportHandshakeErrorContext("Invalid connection id: " + connectionId,
            TransportHandshakeError.ERROR_INVALID_CONNECTION_ID),
            syn.getSource(), isMaxConnectionReached);
        this.isHandshakeError = true;
        return;
      }

      /*
       * New Clients after max License Count are not given any valid clientID. clients anyway close after seeing max
       * connection error message from server.
       */
      licenseLock.lock();
      try {
        if (connectionId.isNewConnection() && !connectionPolicy.isConnectAllowed(connectionId)) {
          isMaxConnectionReached = true;
          this.transport = messageTransportFactory.createNewTransport(syn.getSource(),
              createHandshakeErrorHandler(),
              handshakeMessageFactory, transportListeners);
          this.transport.initConnectionID(transport.getConnectionID());
        } else {
          transport = attachNewConnection(connectionId, syn.getSource());
          isMaxConnectionReached = !connectionPolicy.connectClient(transport.getConnectionID());
        }
      } finally {
        licenseLock.unlock();
      }

      connectionId = transport.getConnectionID();
      // now check that the client side stack and server side stack are both in sync
      short clientStackLayerFlags = syn.getStackLayerFlags();
      short serverStackLayerFlags = this.transport.getCommunicationStackFlags(this.transport);

      // compare the two and send an error if there is a mismatch
      // send the layers present at the server side in the error message
      if ((!isMaxConnectionReached) && (clientStackLayerFlags != serverStackLayerFlags)) {
        String layersPresentInServer = "Layers Present in Server side communication stack: ";
        layersPresentInServer += this.transport.getCommunicationStackNames(this.transport);
        sendSynAck(connectionId, new TransportHandshakeErrorContext(layersPresentInServer,
            TransportHandshakeError.ERROR_STACK_MISMATCH),
            syn.getSource(), isMaxConnectionReached);
        if ((serverStackLayerFlags & NetworkLayer.TYPE_OOO_LAYER) != 0) logger
            .error(NetworkLayer.ERROR_OOO_IN_SERVER_NOT_IN_CLIENT);
        else logger.error(NetworkLayer.ERROR_OOO_IN_CLIENT_NOT_IN_SERVER);
        this.isHandshakeError = true;
        return;
      }
      
      if (!validateTransport.test(this.transport)) {
        sendSynAck(connectionId, new TransportHandshakeErrorContext("connection not allowed", TransportHandshakeError.ERROR_NO_ACTIVE), 
            syn.getSource(), isMaxConnectionReached);
        return;
      }
      sendSynAck(transport.getConnectionID(), syn.getSource(), isMaxConnectionReached);
    }

    private boolean verifySyn(WireProtocolMessage message) {
      return message instanceof TransportHandshakeMessage && (((TransportHandshakeMessage) message).isSyn());
    }

    private void sendSynAck(ConnectionID connectionId, TCConnection source, boolean isMaxConnectionReached) {
      sendSynAck(connectionId, null, source, isMaxConnectionReached);
    }

    /**
     * Connection ID is null. Send a SynAck Error message.
     */
    private void sendSynAck(TransportHandshakeErrorContext errorContext, TCConnection source,
                            boolean isMaxConnectionsReached) {
      Assert.eval(errorContext != null);
      sendSynAck(null, errorContext, source, isMaxConnectionsReached);
    }

    private void sendSynAck(ConnectionID connectionId, TransportHandshakeErrorContext errorContext,
                            TCConnection source, boolean isMaxConnectionsReached) {
      TransportHandshakeMessage synAck;
      boolean isError = (errorContext != null);
      int maxConnections = connectionPolicy.getMaxConnections();
      if (isError) {
        synAck = handshakeMessageFactory.createSynAck(connectionId, errorContext.getErrorType(), errorContext.getMessage(), source, isMaxConnectionsReached,
                                                      maxConnections);
      } else if (activeProvider != null) {
        InetSocketAddress add = new InetSocketAddress(source.getRemoteAddress().getAddress(), source.getRemoteAddress().getPort());
        String active = activeProvider.redirectTo(add);
        if (active != null) {
          synAck = handshakeMessageFactory.createSynAck(connectionId, TransportHandshakeError.ERROR_REDIRECT_CONNECTION, active,
                source, isMaxConnectionsReached, maxConnections);
        } else {
          synAck = handshakeMessageFactory.createSynAck(connectionId, TransportHandshakeError.ERROR_NO_ACTIVE, "no active", 
                source, isMaxConnectionsReached, maxConnections);
        }
      } else {
        synAck = handshakeMessageFactory.createSynAck(connectionId, source, isMaxConnectionsReached, maxConnections);
        source.setTransportEstablished();
      }
      sendMessage(synAck);
    }

    private void sendMessage(WireProtocolMessage message) {
      try {
        transport.sendToConnection(message);
      } catch (IOException ioe) {
        logger.warn("trouble sending message", ioe);
      }
    }
  }

}
