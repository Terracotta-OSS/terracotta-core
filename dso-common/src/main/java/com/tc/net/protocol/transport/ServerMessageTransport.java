/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NetworkStackID;
import com.tc.util.Assert;

public class ServerMessageTransport extends MessageTransportBase {

  private static final TCLogger smtLogger = TCLogging.getLogger(ServerMessageTransport.class);

  public ServerMessageTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_RESTART, handshakeErrorHandler, messageFactory, true, smtLogger);
    this.connectionId = connectionID;
  }

  /**
   * Constructor for when you want a transport that you can specify a connection (e.g., in a server). This constructor
   * will create an open MessageTransport ready for use.
   */
  public ServerMessageTransport(ConnectionID connectionId, TCConnection conn,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_START, handshakeErrorHandler, messageFactory, true, smtLogger);
    this.connectionId = connectionId;
    Assert.assertNotNull(conn);
    wireNewConnection(conn);
  }

  @Override
  protected ConnectionAttacher getConnectionAttacher() {
    if (this.status.isRestart()) {
      return new RestartConnectionAttacher();
    } else return super.getConnectionAttacher();
  }

  @Override
  public NetworkStackID open() {
    throw new UnsupportedOperationException("Server transport doesn't support open()");
  }

  @Override
  protected void receiveTransportMessageImpl(WireProtocolMessage message) {
    boolean notifyTransportConnected = false;
    synchronized (status) {
      if (status.isStart()) {
        notifyTransportConnected = verifyAndHandleAck(message);
        message.recycle();
      } else if (!status.isEstablished()) {
        /*
         * Server Tx can move from START to CLOSED state on client connection restore failure (Client ACK would have
         * reached the server, but worker thread might not have processed it yet and the OOOReconnectTimeout thread
         * pushed the Server Tx to closed state).
         */
        logger.warn("Ignoring the message received for an Un-Established Connection; " + message.getSource() + "; "
                    + message);
        message.recycle();
        return;
      }
    }

    // Transport connected notification happens out here to avoid being done in the status lock scope. This will avoid
    // the deadlock encountered in DEV-7123.
    if (notifyTransportConnected) {
      fireTransportConnectedEvent();
      return;
    }

    // ReceiveToReceiveLayer(message) takes care of verifying the handshake message
    super.receiveToReceiveLayer(message);
  }

  /**
   * @return true if we need to fire the transport connected event
   */
  private boolean verifyAndHandleAck(WireProtocolMessage message) {
    if (!verifyAck(message)) {
      handleHandshakeError(new TransportHandshakeErrorContext("Expected an ACK message but received: " + message,
                                                              TransportHandshakeError.ERROR_HANDSHAKE));
      return false;
    } else {
      handleAck((TransportHandshakeMessage) message);
      return true;
    }
  }

  private void handleAck(TransportHandshakeMessage ack) {
    synchronized (status) {
      Assert.eval(status.isStart());
      Assert.eval("Wrong connection ID: [" + this.connectionId + "] != [" + ack.getConnectionId() + "]",
                  this.connectionId.equals(ack.getConnectionId()));
      status.established();
      ack.getSource().setTransportEstablished();
    }
  }

  private boolean verifyAck(WireProtocolMessage message) {
    return message instanceof TransportHandshakeMessage && ((TransportHandshakeMessage) message).isAck();
  }

  private final class RestartConnectionAttacher implements ConnectionAttacher {

    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection) {
      Assert.assertNull(oldConnection);
      wireNewConnection(newConnection);
    }

  }

}
