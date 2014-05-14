/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.TCInternalError;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCProtocolException;

public class TransportMessageFactoryImpl implements TransportHandshakeMessageFactory, HealthCheckerProbeMessageFactory {
  @Override
  public HealthCheckerProbeMessage createPing(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.PING, connectionId, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  @Override
  public HealthCheckerProbeMessage createPingReply(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.PING_REPLY, connectionId, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  @Override
  public HealthCheckerProbeMessage createTimeCheck(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.TIME_CHECK, connectionId, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  @Override
  public TransportHandshakeMessage createSyn(ConnectionID connectionId, TCConnection source, short stackLayerFlags,
                                             int callbackPort) {
    return createNewMessage(TransportMessageImpl.SYN, connectionId, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE, stackLayerFlags, callbackPort);
  }

  @Override
  public TransportHandshakeMessage createAck(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.ACK, connectionId, null, source, false, 0,
                            TransportHandshakeMessage.NO_CALLBACK_PORT);
  }

  @Override
  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections, int callbackPort) {
    return createNewMessage(TransportMessageImpl.SYN_ACK, connectionId, null, source, isMaxConnectionsExceeded,
                            maxConnections, callbackPort);
  }

  @Override
  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TransportHandshakeError errorContext,
                                                TCConnection source, boolean isMaxConnectionsExceeded,
                                                int maxConnections) {
    return createNewMessage(TransportMessageImpl.SYN_ACK, connectionId, errorContext, source, isMaxConnectionsExceeded,
                            maxConnections, TransportHandshakeMessage.NO_CALLBACK_PORT);
  }

  private static TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                       TransportHandshakeError errorContext, TCConnection source,
                                                       boolean isMaxConnectionsExceeded, int maxConnections, short protocol) {
    return createNewMessage(type, connectionId, errorContext, source, isMaxConnectionsExceeded, maxConnections,
                            protocol, (short) -1, TransportHandshakeMessage.NO_CALLBACK_PORT);
  }

  private static TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                       TransportHandshakeError errorContext, TCConnection source,
                                                       boolean isMaxConnectionsExceeded, int maxConnections, int callbackPort) {
    return createNewMessage(type, connectionId, errorContext, source, isMaxConnectionsExceeded, maxConnections,
                            WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE, (short) -1, callbackPort);
  }

  /**
   * One more parameter is added in createNewMessage so that the syn message that clients send to the server can have
   * the flags set for the present layers in the communication stack All other kinds of packet will have it as -1 and
   * this wouldn't be send to the server
   */
  private static TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                       TransportHandshakeError errorContext, TCConnection source,
                                                       boolean isMaxConnectionsExceeded, int maxConnections, short protocol,
                                                       short stackLayerFlags, int callbackPort) {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();

    bbos.write(TransportMessageImpl.VERSION);
    bbos.write(type);
    connectionId.writeTo(bbos);
    bbos.writeBoolean(isMaxConnectionsExceeded);
    bbos.writeInt(maxConnections);
    bbos.writeShort(stackLayerFlags);
    bbos.writeInt(callbackPort);
    bbos.writeBoolean(errorContext != null);
    if (errorContext != null) {
      short errorType = errorContext.getErrorType();
      bbos.writeShort(errorType);
      if (errorType == TransportHandshakeError.ERROR_STACK_MISMATCH) bbos.writeString(errorContext.getMessage());
      else bbos.writeString(errorContext.toString());
    }
    if (type == TransportMessageImpl.TIME_CHECK) {
      bbos.writeLong(System.currentTimeMillis()); // timestamp
    }

    final WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(protocol);

    final TransportMessageImpl message;
    try {
      message = new TransportMessageImpl(source, header, bbos.toArray());
    } catch (TCProtocolException e) {
      throw new TCInternalError(e);
    }
    return message;
  }

}
