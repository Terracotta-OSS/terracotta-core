/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.net.core.TCConnection;
import com.tc.net.protocol.IllegalReconnectException;
import com.tc.net.protocol.NetworkStackID;
import com.tc.util.Assert;

import java.net.InetSocketAddress;

public class ServerMessageTransport extends MessageTransportBase {

  private static final Logger smtLogger = LoggerFactory.getLogger(ServerMessageTransport.class);

  public ServerMessageTransport(TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_RESTART, handshakeErrorHandler, messageFactory, smtLogger);
  }

  /**
   * Constructor for when you want a transport that you can specify a connection (e.g., in a server). This constructor
   * will create an open MessageTransport ready for use.
   */
  public ServerMessageTransport(TCConnection conn,
                                TransportHandshakeErrorHandler handshakeErrorHandler,
                                TransportHandshakeMessageFactory messageFactory) {
    super(MessageTransportState.STATE_START, handshakeErrorHandler, messageFactory, smtLogger);
    Assert.assertNotNull(conn);
    wireNewConnection(conn);
  }

  @Override
  public void attachNewConnection(TCConnection newConnection) throws IllegalReconnectException {
    if (!this.status.isRestart()) {
      // servers transports can only restart once
      throw new IllegalReconnectException();
    }
    Assert.assertNull(getConnection());
    wireNewConnection(newConnection);
    logger.debug("reconnect connection attach {} {}", this.getConnectionID().getClientID(), getConnection());
  }

  @Override
  public NetworkStackID open(InetSocketAddress serverAddress) {
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
    if (status.isEstablished()) {
      // do nothing, just receive
    } else {
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
    }

    if (recycleAndReturn) {
      if (notifyTransportConnected) {
        // Transport connected notification happens out here to avoid being done in the status lock scope. This will
        // avoid the deadlock encountered in DEV-7123.
        fireTransportConnectedEvent();
      }
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
      Assert.eval("Wrong connection ID: [" + getConnectionID() + "] != [" + ack.getConnectionId() + "]",
                  !getConnectionID().isNull() || getConnectionID().equals(ack.getConnectionId()));
      status.established();
    }
  }

  private boolean verifyAck(WireProtocolMessage message) {
    return message instanceof TransportHandshakeMessage && ((TransportHandshakeMessage) message).isAck();
  }

  @Override
  public String toString() {
    return "ServerMessageTransport{connection=" + getConnection() + '}';
  }
}
