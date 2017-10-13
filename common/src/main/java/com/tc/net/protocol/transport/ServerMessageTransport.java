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
import org.slf4j.LoggerFactory;

import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.protocol.NetworkStackID;
import com.tc.util.Assert;

public class ServerMessageTransport extends MessageTransportBase {

  private static final Logger smtLogger = LoggerFactory.getLogger(ServerMessageTransport.class);

  public ServerMessageTransport(TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_RESTART, handshakeErrorHandler, messageFactory, true, smtLogger);
  }

  /**
   * Constructor for when you want a transport that you can specify a connection (e.g., in a server). This constructor
   * will create an open MessageTransport ready for use.
   */
  public ServerMessageTransport(TCConnection conn,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_START, handshakeErrorHandler, messageFactory, true, smtLogger);
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
  public NetworkStackID open(ConnectionInfo info) {
    throw new UnsupportedOperationException("Server transport doesn't support open()");
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("Server transport doesn't support reset()");
  }

  @Override
  protected void receiveTransportMessageImpl(WireProtocolMessage message) {
    boolean notifyTransportConnected = false;
    boolean recycleAndReturn = false;
    synchronized (status) {
      if (status.isConnected()) {
        recycleAndReturn = true;
        notifyTransportConnected = verifyAndHandleAck(message);
      } else if (!status.isEstablished()) {
        /*
         * Server Tx can move from START to CLOSED state on client connection restore failure (Client ACK would have
         * reached the server, but worker thread might not have processed it yet and the OOOReconnectTimeout thread
         * pushed the Server Tx to closed state).
         */
        logger.debug("Ignoring the message received for an Un-Established Connection; " + message.getSource() + "; "
                    + message);
        recycleAndReturn = true;
      }
    }

    if (recycleAndReturn) {
      if (notifyTransportConnected) {
        // Transport connected notification happens out here to avoid being done in the status lock scope. This will
        // avoid the deadlock encountered in DEV-7123.
        fireTransportConnectedEvent();
      }
      message.recycle();
      return;
    } else {
      // ReceiveToReceiveLayer(message) takes care of verifying the handshake message
      super.receiveToReceiveLayer(message);
    }
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
      Assert.eval(status.isConnected());
      Assert.eval("Wrong connection ID: [" + getConnectionId() + "] != [" + ack.getConnectionId() + "]",
                  !getConnectionId().isValid() || getConnectionId().equals(ack.getConnectionId()));
      status.established();
      ack.getSource().setTransportEstablished();
    }
  }

  private boolean verifyAck(WireProtocolMessage message) {
    return message instanceof TransportHandshakeMessage && ((TransportHandshakeMessage) message).isAck();
  }

  private final class RestartConnectionAttacher implements ConnectionAttacher {

    @Override
    public void attachNewConnection(TCConnectionEvent closeEvent, TCConnection oldConnection, TCConnection newConnection) {
      Assert.assertNull(oldConnection);
      wireNewConnection(newConnection);
      log("Attaching new connection to transport: " + newConnection);
    }

  }

}
