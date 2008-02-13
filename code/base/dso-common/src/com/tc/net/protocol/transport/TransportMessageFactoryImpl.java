/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.TCInternalError;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCProtocolException;

public class TransportMessageFactoryImpl implements TransportHandshakeMessageFactory, HealthCheckerProbeMessageFactory {
  public static final ConnectionID DEFAULT_ID = ConnectionID.NULL_ID;

  public HealthCheckerProbeMessage createPing(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.PING, connectionId, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  public HealthCheckerProbeMessage createPingReply(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.PING_REPLY, connectionId, null, source, false, 0,
                            WireProtocolHeader.PROTOCOL_HEALTHCHECK_PROBES);
  }

  public TransportHandshakeMessage createSyn(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.SYN, connectionId, null, source, false, 0);
  }

  public TransportHandshakeMessage createAck(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportMessageImpl.ACK, connectionId, null, source, false, 0);
  }

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections) {
    return createSynAck(connectionId, null, source, isMaxConnectionsExceeded, maxConnections);
  }

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TransportHandshakeErrorContext errorContext,
                                                TCConnection source, boolean isMaxConnectionsExceeded,
                                                int maxConnections) {
    return createNewMessage(TransportMessageImpl.SYN_ACK, connectionId, errorContext, source, isMaxConnectionsExceeded,
                            maxConnections);
  }

  private TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                TransportHandshakeErrorContext errorContext, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections) {
    return createNewMessage(type, connectionId, errorContext, source, isMaxConnectionsExceeded, maxConnections,
                            WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);
  }

  private TransportMessageImpl createNewMessage(byte type, ConnectionID connectionId,
                                                TransportHandshakeErrorContext errorContext, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections, short protocol) {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();

    bbos.write(TransportMessageImpl.VERSION_1);
    bbos.write(type);
    bbos.writeString(connectionId.getID());
    bbos.writeBoolean(isMaxConnectionsExceeded);
    bbos.writeInt(maxConnections);
    bbos.writeBoolean(errorContext != null);
    if (errorContext != null) bbos.writeString(errorContext.toString());

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
