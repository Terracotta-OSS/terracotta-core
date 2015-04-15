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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnection;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.RejectReconnectionException;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.tcm.msgs.CommsMessageFactory;
import com.tc.util.Assert;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides network stacks on the server side
 */
public class ServerStackProvider implements NetworkStackProvider, MessageTransportListener, ProtocolAdaptorFactory {
  private static final TCLogger logger = TCLogging.getLogger(ServerStackProvider.class);

  private final Map<ConnectionID, NetworkStackHarness> harnesses          = Maps.newConcurrentMap();
  private final NetworkStackHarnessFactory       harnessFactory;
  private final ServerMessageChannelFactory      channelFactory;
  private final TransportHandshakeMessageFactory handshakeMessageFactory;
  private final ConnectionIDFactory              connectionIdFactory;
  private final ConnectionPolicy                 connectionPolicy;
  private final WireProtocolAdaptorFactory       wireProtocolAdaptorFactory;
  private final WireProtocolMessageSink          wireProtoMsgsink;
  private final TCSecurityManager                securityManager;
  private final MessageTransportFactory          messageTransportFactory;
  private final List<MessageTransportListener>   transportListeners = Lists.newArrayList();
  private final ReentrantLock                    licenseLock;
  private final String                           commsMgrName;

  // used only in test
  public ServerStackProvider(Set initialConnectionIDs, NetworkStackHarnessFactory harnessFactory,
                             ServerMessageChannelFactory channelFactory,
                             MessageTransportFactory messageTransportFactory,
                             TransportHandshakeMessageFactory handshakeMessageFactory,
                             ConnectionIDFactory connectionIdFactory, ConnectionPolicy connectionPolicy,
                             WireProtocolAdaptorFactory wireProtocolAdaptorFactory, ReentrantLock licenseLock) {
    this(initialConnectionIDs, harnessFactory, channelFactory, messageTransportFactory,
         handshakeMessageFactory, connectionIdFactory, connectionPolicy, wireProtocolAdaptorFactory, null, licenseLock,
         CommunicationsManager.COMMSMGR_SERVER, null);
  }

  public ServerStackProvider(Set initialConnectionIDs, NetworkStackHarnessFactory harnessFactory,
                             ServerMessageChannelFactory channelFactory,
                             MessageTransportFactory messageTransportFactory,
                             TransportHandshakeMessageFactory handshakeMessageFactory,
                             ConnectionIDFactory connectionIdFactory, ConnectionPolicy connectionPolicy,
                             WireProtocolAdaptorFactory wireProtocolAdaptorFactory,
                             WireProtocolMessageSink wireProtoMsgSink, ReentrantLock licenseLock,
                             final String commsMgrName, final TCSecurityManager securityManager) {
    this.messageTransportFactory = messageTransportFactory;
    this.connectionPolicy = connectionPolicy;
    this.wireProtocolAdaptorFactory = wireProtocolAdaptorFactory;
    this.wireProtoMsgsink = wireProtoMsgSink;
    this.securityManager = securityManager;
    Assert.assertNotNull(harnessFactory);
    this.harnessFactory = harnessFactory;
    this.channelFactory = channelFactory;
    this.handshakeMessageFactory = handshakeMessageFactory;
    this.connectionIdFactory = connectionIdFactory;
    this.transportListeners.add(this);
    Assert.assertNotNull(licenseLock);
    this.licenseLock = licenseLock;
    this.commsMgrName = commsMgrName;
    for (final Object initialConnectionID : initialConnectionIDs) {
      ConnectionID connectionID = (ConnectionID)initialConnectionID;
      logger.info("Preparing comms stack for previously connected client: " + connectionID);
      newStackHarness(connectionID, messageTransportFactory.createNewTransport(connectionID,
          createHandshakeErrorHandler(),
          handshakeMessageFactory,
          transportListeners));
    }
  }

  @Override
  public MessageTransport attachNewConnection(ConnectionID connectionId, TCConnection connection)
      throws RejectReconnectionException {
    Assert.assertNotNull(connection);

    final NetworkStackHarness harness;
    final MessageTransport rv;
    ConnectionID ourConnectionId;
    if (connectionId.isNewConnection()) {
      ourConnectionId = connectionIdFactory.populateConnectionID(connectionId);

      rv = messageTransportFactory.createNewTransport(ourConnectionId, connection, createHandshakeErrorHandler(),
          handshakeMessageFactory, transportListeners);
      newStackHarness(ourConnectionId, rv);
    } else {
      harness = harnesses.get(connectionId);

      if (harness == null) {
        throw new RejectReconnectionException("Stack for " + connectionId +" not found.", connection.getRemoteAddress());
      } else {
        try {
          rv = harness.attachNewConnection(connection);
        } catch (IllegalReconnectException e) {
          logger.warn("Client attempting an illegal reconnect for id " + connectionId + ", " + connection);
          throw new RejectReconnectionException("Illegal reconnect attempt from " + connectionId + ".", connection.getRemoteAddress());
        }
        connectionIdFactory.restoreConnectionId(connectionId);
      }
    }

    return rv;
  }

  private void newStackHarness(ConnectionID id, MessageTransport transport) {
    final NetworkStackHarness harness;
    harness = harnessFactory.createServerHarness(channelFactory, transport, new MessageTransportListener[] { this });
    harness.finalizeStack();
    Object previous = harnesses.put(id, harness);
    if (previous != null) { throw new AssertionError("previous is " + previous + "connectionID:" + id + "new is"
                                                     + harness); }
  }

  private TransportHandshakeErrorHandler createHandshakeErrorHandler() {
    return new TransportHandshakeErrorHandler() {
      @Override
      public void handleHandshakeError(TransportHandshakeErrorContext thec) {
        logger.info(thec.getMessage());
      }
    };
  }

  NetworkStackHarness removeNetworkStack(ConnectionID connectionId) {
    return harnesses.remove(connectionId);
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
  public void notifyTransportDisconnected(MessageTransport transport, final boolean forcedDisconnect) {
    /*
     * Even for temporary disconnects, we need to keep proper accounting. Otherwise, the same client reconnect may fail.
     */
    this.connectionPolicy.clientDisconnected(transport.getConnectionId());
  }

  private void close(ConnectionID connectionId) {
    NetworkStackHarness harness = removeNetworkStack(connectionId);
    if (harness == null) { throw new AssertionError(
                                                    "Receive a transport closed event for a transport that isn't in the map :"
                                                        + connectionId); }
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
    close(transport.getConnectionId());
    if (!transport.getConnectionId().isJvmIDNull()) this.connectionPolicy.clientDisconnected(transport
        .getConnectionId());
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
      MessageSink sink = new MessageSink(createHandshakeErrorHandler(), this.commsMgrName);
      return this.wireProtocolAdaptorFactory.newWireProtocolAdaptor(sink);
    }
  }

  /*********************************************************************************************************************
   * private stuff
   */

  class MessageSink implements WireProtocolMessageSink {
    private final TransportHandshakeErrorHandler handshakeErrorHandler;
    private final String                         commsManagerName;
    private volatile boolean                     isSynReceived    = false;
    private volatile boolean                     isHandshakeError = false;
    private volatile MessageTransport            transport;

    private MessageSink(TransportHandshakeErrorHandler handshakeErrorHandler, String commsMgrName) {
      this.handshakeErrorHandler = handshakeErrorHandler;
      this.commsManagerName = commsMgrName;
    }

    @Override
    public void putMessage(WireProtocolMessage message) {
      if (!isSynReceived) {
        synchronized (this) {
          if (!isSynReceived) {
            isSynReceived = verifyAndHandleSyn(message);
            message.recycle();
            return;
          }
        }
      }
      if (!isHandshakeError) {
        this.transport.receiveTransportMessage(message);
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
          String errorMessage = CommsMessageFactory.createReconnectRejectMessage(this.commsManagerName,
                                                                                 new Object[] { e.getMessage() });
          // send connection rejected SycAck back to L1
          // since stack no found, create a temp transport for sending sycAck message back
          this.transport = messageTransportFactory.createNewTransport(((SynMessage) message).getConnectionId(),
                                                                      ((SynMessage) message).getSource(),
                                                                      createHandshakeErrorHandler(),
                                                                      handshakeMessageFactory, transportListeners);
          sendSynAck(((SynMessage) message).getConnectionId(),
                     new TransportHandshakeErrorContext(errorMessage,
                                                        TransportHandshakeError.ERROR_RECONNECTION_REJECTED),
                     ((SynMessage) message).getSource(), false);

          handleHandshakeError(new TransportHandshakeErrorContext(errorMessage, e));
        }
      }
      return isSynced;
    }

    private void handleHandshakeError(TransportHandshakeErrorContext ctxt) {
      this.isHandshakeError = true;
      this.handshakeErrorHandler.handleHandshakeError(ctxt);
    }

    private void handleSyn(SynMessage syn) throws RejectReconnectionException {
      ConnectionID connectionId = syn.getConnectionId();
      boolean isMaxConnectionReached = false;

      if (connectionId == null) {
        this.transport = messageTransportFactory.createNewTransport(connectionId, syn.getSource(),
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
          this.transport = messageTransportFactory.createNewTransport(connectionId, syn.getSource(),
              createHandshakeErrorHandler(),
              handshakeMessageFactory, transportListeners);
        } else {
          transport = attachNewConnection(connectionId, syn.getSource());
          ConnectionID sentConnectionId = connectionId;
          ConnectionID transportConnectionId = transport.getConnectionId();
          // Update the connection ID with the new channel id and server id from server
          connectionId = new ConnectionID(sentConnectionId.getJvmID(), transportConnectionId.getChannelID(),
              transportConnectionId.getServerID(), sentConnectionId.getUsername(), sentConnectionId.getPassword(),
              sentConnectionId.getProductId());
          // populate the jvmid on the server copy of the connection id if it's null
          if (transportConnectionId.isJvmIDNull()) {
            transport.initConnectionID(new ConnectionID(sentConnectionId.getJvmID(), connectionId.getChannelID(),
                connectionId.getServerID()));
          }
          isMaxConnectionReached = !connectionPolicy.connectClient(connectionId);
        }
      } finally {
        licenseLock.unlock();
      }

      this.transport.setRemoteCallbackPort(syn.getCallbackPort());
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
      Principal principal = null;
      if (securityManager != null) {
        if (!connectionId.isSecured()) {
          logger.fatal("Security is enabled here on the server, but we didn't get credentials on the handshake!");
          this.isHandshakeError = true;
          return;
        } else {
          if ((principal = securityManager.authenticate(connectionId.getUsername(), connectionId.getPassword())) == null) {
            logger.fatal("Authentication failed for user " + connectionId.getUsername()
                         + " with pw (" + connectionId.getPassword().length + "): " + new String(connectionId.getPassword()));
            this.isHandshakeError = true;
            return;
          }
        }
      }
      logger.info("User " + principal + " successfully authenticated");
      // todo store principal ?
      sendSynAck(connectionId, syn.getSource(), isMaxConnectionReached);
    }

    private boolean verifySyn(WireProtocolMessage message) {
      return message instanceof TransportHandshakeMessage && (((TransportHandshakeMessage) message).isSyn());
    }

    private void sendSynAck(ConnectionID connectionId, TCConnection source, boolean isMaxConnectionReached) {
      source.addWeight(MessageTransport.CONNWEIGHT_TX_HANDSHAKED);
      sendSynAck(connectionId, null, source, isMaxConnectionReached);
    }

    /**
     * Connection ID is null. Send a SynAck Error message.
     */
    private void sendSynAck(final TransportHandshakeErrorContext errorContext, final TCConnection source,
                            final boolean isMaxConnectionsReached) {
      Assert.eval(errorContext != null);
      sendSynAck(null, errorContext, source, isMaxConnectionsReached);
    }

    private void sendSynAck(ConnectionID connectionId, TransportHandshakeErrorContext errorContext,
                            TCConnection source, boolean isMaxConnectionsReached) {
      TransportHandshakeMessage synAck;
      boolean isError = (errorContext != null);
      int maxConnections = connectionPolicy.getMaxConnections();
      if (isError) {
        synAck = handshakeMessageFactory.createSynAck(connectionId, errorContext, source, isMaxConnectionsReached,
                                                      maxConnections);
      } else {
        int callbackPort = source.getLocalAddress().getPort();
        synAck = handshakeMessageFactory.createSynAck(connectionId, source, isMaxConnectionsReached, maxConnections,
                                                      callbackPort);
      }
      sendMessage(synAck);
    }

    private void sendMessage(WireProtocolMessage message) {
      transport.sendToConnection(message);
    }
  }

}
