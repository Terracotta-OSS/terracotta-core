/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.NetworkStackHarness;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.StackNotFoundException;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.ServerMessageChannelFactory;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides network statcks on the server side
 */
public class ServerStackProvider implements NetworkStackProvider, MessageTransportListener, ProtocolAdaptorFactory {

  private final Map                              harnesses          = new ConcurrentHashMap();
  private final NetworkStackHarnessFactory       harnessFactory;
  private final ServerMessageChannelFactory      channelFactory;
  private final TransportHandshakeMessageFactory handshakeMessageFactory;
  private final ConnectionIDFactory              connectionIdFactory;
  private final ConnectionPolicy                 connectionPolicy;
  private final WireProtocolAdaptorFactory       wireProtocolAdaptorFactory;
  private final MessageTransportFactory          messageTransportFactory;
  private final List                             transportListeners = new ArrayList(1);
  private final TCLogger                         logger;
  private final TCLogger                         consoleLogger      = CustomerLogging.getConsoleLogger();

  public ServerStackProvider(TCLogger logger, Set initialConnectionIDs, NetworkStackHarnessFactory harnessFactory,
                             ServerMessageChannelFactory channelFactory,
                             MessageTransportFactory messageTransportFactory,
                             TransportHandshakeMessageFactory handshakeMessageFactory,
                             ConnectionIDFactory connectionIdFactory, ConnectionPolicy connectionPolicy,
                             WireProtocolAdaptorFactory wireProtocolAdaptorFactory) {
    this.messageTransportFactory = messageTransportFactory;
    this.connectionPolicy = connectionPolicy;
    this.wireProtocolAdaptorFactory = wireProtocolAdaptorFactory;
    Assert.assertNotNull(harnessFactory);
    this.harnessFactory = harnessFactory;
    this.channelFactory = channelFactory;
    this.handshakeMessageFactory = handshakeMessageFactory;
    this.connectionIdFactory = connectionIdFactory;
    this.transportListeners.add(this);
    this.logger = logger;
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
      throws StackNotFoundException {
    Assert.assertNotNull(connection);

    final NetworkStackHarness harness;
    final MessageTransport rv;
    if (connectionId.isNull()) {
      connectionId = connectionIdFactory.nextConnectionId();

      rv = messageTransportFactory.createNewTransport(connectionId, connection, createHandshakeErrorHandler(),
                                                      handshakeMessageFactory, transportListeners);
      newStackHarness(connectionId, rv);
    } else {
      harness = (NetworkStackHarness) harnesses.get(connectionId);

      if (harness == null) {
        throw new StackNotFoundException(connectionId);
      } else {
        rv = harness.attachNewConnection(connection);
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
    return new TransportHandshakeErrorHandler() {

      public void handleHandshakeError(TransportHandshakeErrorContext e) {
        consoleLogger.info(e.getMessage());
        logger.info(e.getMessage());
      }

      public void handleHandshakeError(TransportHandshakeErrorContext e, TransportHandshakeMessage m) {
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
  public void notifyTransportDisconnected(MessageTransport transport) {
    // Currenly we dont care about this event here. In AbstractMessageChannel in the server, this event closes the
    // channel
    // so effectively a disconnected transport means a closed channel in the server. When we later implement clients
    // reconnect
    // this will change and this will trigger a reconnect window for that client here.
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
    this.connectionPolicy.clientDisconnected();
  }

  /*********************************************************************************************************************
   * ProtocolAdaptorFactory interface
   */

  public TCProtocolAdaptor getInstance() {
    MessageSink sink = new MessageSink(createHandshakeErrorHandler());
    return this.wireProtocolAdaptorFactory.newWireProtocolAdaptor(sink);
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

    public void putMessage(WireProtocolMessage message) {
      if (!isSynReceived) {
        synchronized (this) {
          if (!isSynReceived) {
            isSynReceived = true;
            verifyAndHandleSyn(message);
            message.recycle();
            return;
          }
        }
      }
      if (!isHandshakeError) {
        this.transport.receiveTransportMessage(message);
      }
    }

    private void verifyAndHandleSyn(WireProtocolMessage message) {
      if (!verifySyn(message)) {
        handleHandshakeError(new TransportHandshakeErrorContext("Expected a SYN message but received: " + message));
      } else {
        try {
          handleSyn((SynMessage) message);
        } catch (StackNotFoundException e) {
          handleHandshakeError(new TransportHandshakeErrorContext(
                                                                  "Unable to find communications stack. "
                                                                      + e.getMessage()
                                                                      + ". This is usually caused by a client from a prior run trying to illegally reconnect to the server."
                                                                      + " While that client is being rejected, everything else should proceed as normal. ",
                                                                  e));
        }
      }
    }

    private void handleHandshakeError(TransportHandshakeErrorContext ctxt) {
      this.isHandshakeError = true;
      this.handshakeErrorHandler.handleHandshakeError(ctxt);
    }

    private void handleSyn(SynMessage syn) throws StackNotFoundException {
      ConnectionID connectionId = syn.getConnectionId();

      if (connectionId == null) {
        sendSynAck(connectionId, new TransportHandshakeErrorContext("Invalid connection id: " + connectionId), syn
            .getSource());
        this.isHandshakeError = true;
        return;
      }

      this.transport = attachNewConnection(connectionId, syn.getSource());
      connectionId = this.transport.getConnectionId();
      sendSynAck(connectionId, syn.getSource());
    }

    private boolean verifySyn(WireProtocolMessage message) {
      return message instanceof TransportHandshakeMessage && ((TransportHandshakeMessage) message).isSyn();
    }

    private void sendSynAck(ConnectionID connectionId, TCConnection source) {
      sendSynAck(connectionId, null, source);
    }

    private void sendSynAck(ConnectionID connectionId, TransportHandshakeErrorContext errorContext, TCConnection source) {
      TransportHandshakeMessage synAck;
      boolean isError = (errorContext != null);
      int maxConnections = connectionPolicy.getMaxConnections();
      connectionPolicy.clientConnected();
      // NOTE: There's a race here which should be ok, since it doesn't matter which client gets told there are
      // no more connections left...
      boolean isMaxConnectionsExceeded = connectionPolicy.maxConnectionsExceeded();
      if (isError) {
        synAck = handshakeMessageFactory.createSynAck(connectionId, errorContext, source, isMaxConnectionsExceeded,
                                                      maxConnections);
      } else {
        synAck = handshakeMessageFactory.createSynAck(connectionId, source, isMaxConnectionsExceeded, maxConnections);
      }
      sendMessage(synAck);
    }

    private void sendMessage(WireProtocolMessage message) {
      transport.sendToConnection(message);
    }
  }

}
