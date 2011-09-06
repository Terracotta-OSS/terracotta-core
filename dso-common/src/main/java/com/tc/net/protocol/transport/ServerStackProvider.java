/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkLayer;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.StackNotFoundException;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.net.protocol.tcm.msgs.CommsMessageFactory;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides network stacks on the server side
 */
public class ServerStackProvider implements NetworkStackProvider, MessageTransportListener, ProtocolAdaptorFactory {

  private final Map                              harnesses          = new ConcurrentHashMap();
  private final NetworkStackHarnessFactory       harnessFactory;
  private final ServerMessageChannelFactory      channelFactory;
  private final TransportHandshakeMessageFactory handshakeMessageFactory;
  private final ConnectionIDFactory              connectionIdFactory;
  private final ConnectionPolicy                 connectionPolicy;
  private final WireProtocolAdaptorFactory       wireProtocolAdaptorFactory;
  private final WireProtocolMessageSink          wireProtoMsgsink;
  private final MessageTransportFactory          messageTransportFactory;
  private final List                             transportListeners = new ArrayList(1);
  private final TCLogger                         logger;
  private final TCLogger                         consoleLogger      = CustomerLogging.getConsoleLogger();
  private final ReentrantLock                    licenseLock;
  private final String                           commsMgrName;

  // used only in test
  public ServerStackProvider(TCLogger logger, Set initialConnectionIDs, NetworkStackHarnessFactory harnessFactory,
                             ServerMessageChannelFactory channelFactory,
                             MessageTransportFactory messageTransportFactory,
                             TransportHandshakeMessageFactory handshakeMessageFactory,
                             ConnectionIDFactory connectionIdFactory, ConnectionPolicy connectionPolicy,
                             WireProtocolAdaptorFactory wireProtocolAdaptorFactory, ReentrantLock licenseLock) {
    this(logger, initialConnectionIDs, harnessFactory, channelFactory, messageTransportFactory,
         handshakeMessageFactory, connectionIdFactory, connectionPolicy, wireProtocolAdaptorFactory, null, licenseLock,
         CommunicationsManager.COMMSMGR_SERVER);
  }

  public ServerStackProvider(TCLogger logger, Set initialConnectionIDs, NetworkStackHarnessFactory harnessFactory,
                             ServerMessageChannelFactory channelFactory,
                             MessageTransportFactory messageTransportFactory,
                             TransportHandshakeMessageFactory handshakeMessageFactory,
                             ConnectionIDFactory connectionIdFactory, ConnectionPolicy connectionPolicy,
                             WireProtocolAdaptorFactory wireProtocolAdaptorFactory,
                             WireProtocolMessageSink wireProtoMsgSink, ReentrantLock licenseLock,
                             final String commsMgrName) {
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
    this.logger = logger;
    Assert.assertNotNull(licenseLock);
    this.licenseLock = licenseLock;
    this.commsMgrName = commsMgrName;
    for (Iterator i = initialConnectionIDs.iterator(); i.hasNext();) {
      ConnectionID connectionID = (ConnectionID) i.next();
      logger.info("Preparing comms stack for previously connected client: " + connectionID);
      newStackHarness(connectionID, messageTransportFactory.createNewTransport(connectionID,
                                                                               createHandshakeErrorHandler(),
                                                                               handshakeMessageFactory,
                                                                               transportListeners));
    }
  }

  public MessageTransport attachNewConnection(ConnectionID connectionId, TCConnection connection)
      throws StackNotFoundException, IllegalReconnectException {
    Assert.assertNotNull(connection);

    final NetworkStackHarness harness;
    final MessageTransport rv;
    if (connectionId.isNewConnection()) {
      if (connectionId.getChannelID() == ChannelID.NULL_ID.toLong()) {
        connectionId = connectionIdFactory.nextConnectionId(connectionId.getJvmID());
      } else {
        connectionId = connectionIdFactory.makeConnectionId(connectionId.getJvmID(), connectionId.getChannelID());
      }

      rv = messageTransportFactory.createNewTransport(connectionId, connection, createHandshakeErrorHandler(),
                                                      handshakeMessageFactory, transportListeners);
      newStackHarness(connectionId, rv);
    } else {
      harness = (NetworkStackHarness) harnesses.get(connectionId);

      if (harness == null) {
        throw new StackNotFoundException(connectionId, connection.getRemoteAddress());
      } else {
        rv = harness.attachNewConnection(connection);
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
    if (previous != null) { throw new AssertionError("previous is " + previous); }
  }

  private TransportHandshakeErrorHandler createHandshakeErrorHandler() {
    if (this.commsMgrName.equals(CommunicationsManager.COMMSMGR_GROUPS)) { return new TransportHandshakeErrorHandler() {
      public void handleHandshakeError(TransportHandshakeErrorContext e) {
        logger.info(e.getMessage());
      }
    }; }

    return new TransportHandshakeErrorHandler() {

      public void handleHandshakeError(TransportHandshakeErrorContext e) {
        consoleLogger.info(e.getMessage());
        logger.info(e.getMessage());
      }

    };
  }

  NetworkStackHarness removeNetworkStack(ConnectionID connectionId) {
    return (NetworkStackHarness) harnesses.remove(connectionId);
  }

  /*********************************************************************************************************************
   * MessageTransportListener methods.
   */
  public void notifyTransportConnected(MessageTransport transport) {
    // don't care
  }

  /**
   * A client disconnected.
   */
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

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    // don't care
  }

  /**
   * The connection was closed. The client is never allowed to reconnect. Removes stack associated with the given
   * transport from the map of managed stacks.
   */
  public void notifyTransportClosed(MessageTransport transport) {
    close(transport.getConnectionId());
    if (!transport.getConnectionId().isJvmIDNull()) this.connectionPolicy.clientDisconnected(transport
        .getConnectionId());
  }

  public void notifyTransportReconnectionRejected(MessageTransport transport) {
    // NOP
  }

  /*********************************************************************************************************************
   * ProtocolAdaptorFactory interface
   */

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
        } catch (StackNotFoundException e) {
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

    private void handleSyn(SynMessage syn) throws StackNotFoundException {

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
        if (connectionId.isNewConnection() && connectionPolicy.isMaxConnectionsReached()) {
          isMaxConnectionReached = true;
          this.transport = messageTransportFactory.createNewTransport(connectionId, syn.getSource(),
                                                                      createHandshakeErrorHandler(),
                                                                      handshakeMessageFactory, transportListeners);
        } else {
          try {
            this.transport = attachNewConnection(connectionId, syn.getSource());
          } catch (IllegalReconnectException e) {
            logger.warn("Client attempting an illegal reconnect for id " + connectionId + ", " + syn.getSource());
            return;
          }
          ConnectionID sentConnectionId = connectionId;
          connectionId = this.transport.getConnectionId();
          if (connectionId.isJvmIDNull()) {
            connectionId = new ConnectionID(sentConnectionId.getJvmID(), connectionId.getChannelID(),
                                            connectionId.getServerID());
            this.transport.initConnectionID(connectionId);
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
