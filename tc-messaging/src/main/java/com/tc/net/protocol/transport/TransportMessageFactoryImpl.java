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

import com.tc.exception.TCInternalError;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCProtocolException;
import static com.tc.net.protocol.transport.TransportHandshakeMessage.NO_CALLBACK_PORT;

public class TransportMessageFactoryImpl implements TransportHandshakeMessageFactory, HealthCheckerProbeMessageFactory {
  @Override
  public HealthCheckerProbeMessage createPing(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.PING, connectionId, null, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  @Override
  public HealthCheckerProbeMessage createPingReply(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.PING_REPLY, connectionId, null, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  @Override
  public HealthCheckerProbeMessage createTimeCheck(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.TIME_CHECK, connectionId, null, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  @Override
  public TransportHandshakeMessage createSyn(ConnectionID connectionId, TCConnection source, short stackLayerFlags) {
    return createNewMessage(TransportMessageImpl.SYN, connectionId, null, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE, stackLayerFlags);
  }

  @Override
  public TransportHandshakeMessage createAck(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.ACK, connectionId, null, null, source, false, 0);
  }

  @Override
  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections) {
    return createNewMessage(TransportMessageImpl.SYN_ACK, connectionId, null, null, source, isMaxConnectionsExceeded,
                            maxConnections);
  }

  @Override
  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TransportHandshakeError errorContext, String message,
                                                TCConnection source, boolean isMaxConnectionsExceeded,
                                                int maxConnections) {
    return createNewMessage(TransportMessageImpl.SYN_ACK, connectionId, errorContext, message, source, isMaxConnectionsExceeded,
                            maxConnections);
  }

  private static TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                       TransportHandshakeError errorContext, String message, TCConnection source,
                                                       boolean isMaxConnectionsExceeded, int maxConnections, short protocol) {
    return createNewMessage(type, connectionId, errorContext, message, source, isMaxConnectionsExceeded, maxConnections,
                            protocol, (short) -1);
  }

  private static TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                       TransportHandshakeError errorContext, String message, TCConnection source,
                                                       boolean isMaxConnectionsExceeded, int maxConnections) {
    return createNewMessage(type, connectionId, errorContext, message, source, isMaxConnectionsExceeded, maxConnections,
                            WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE, (short) -1);
  }

  /**
   * One more parameter is added in createNewMessage so that the syn message that clients send to the server can have
   * the flags set for the present layers in the communication stack All other kinds of packet will have it as -1 and
   * this wouldn't be send to the server
   */
  @SuppressWarnings("resource")
  private static TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                       TransportHandshakeError errorContext, String message, TCConnection source,
                                                       boolean isMaxConnectionsExceeded, int maxConnections, short protocol,
                                                       short stackLayerFlags) {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();

    bbos.write(TransportMessageImpl.VERSION);
    bbos.write(type);
    connectionId.writeTo(bbos);
    bbos.writeBoolean(isMaxConnectionsExceeded);
    bbos.writeInt(maxConnections);
    bbos.writeShort(stackLayerFlags);
    bbos.writeInt(NO_CALLBACK_PORT);  //  unused callback port for compatibility
    bbos.writeBoolean(errorContext != null);
    if (errorContext != null) {
      short errorType = errorContext.getErrorType();
      bbos.writeShort(errorType);
      bbos.writeString(message);
    }
    if (type == TransportMessageImpl.TIME_CHECK) {
      bbos.writeLong(System.currentTimeMillis()); // timestamp
    }

    final WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(protocol);

    final TransportMessageImpl packet;
    try {
      bbos.close();
      packet = new TransportMessageImpl(source, header, bbos.accessBuffers());
    } catch (TCProtocolException e) {
      throw new TCInternalError(e);
    }
    return packet;
  }

}
