/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

public class MockTransportMessageFactory implements TransportHandshakeMessageFactory {

  public TransportHandshakeMessage    syn;
  public TransportHandshakeMessage    ack;
  public TransportHandshakeMessage    synAck;

  public final NoExceptionLinkedQueue createSynCalls    = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue createAckCalls    = new NoExceptionLinkedQueue();
  public final NoExceptionLinkedQueue createSynAckCalls = new NoExceptionLinkedQueue();

  public TransportHandshakeMessage createSyn(ConnectionID connectionId, TCConnection source, short stackLayerFlags,
                                             int callbackPort) {
    createSynCalls.put(new Object[] { connectionId, source });
    return this.syn;
  }

  public TransportHandshakeMessage createAck(ConnectionID connectionId, TCConnection source) {
    createAckCalls.put(new CallContext(connectionId, null, source, null, null));
    return this.ack;
  }

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections, int callbackPort) {
    return createSynAck(connectionId, null, source, isMaxConnectionsExceeded, maxConnections);
  }

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TransportHandshakeErrorContext errorContext,
                                                TCConnection source, boolean isMaxConnectionsExceeded,
                                                int maxConnections) {
    createSynAckCalls.put(new CallContext(connectionId, errorContext, source, new Boolean(isMaxConnectionsExceeded),
                                          new Integer(maxConnections)));
    return this.synAck;
  }

  public static final class CallContext {
    private final ConnectionID                   connectionId;
    private final TCConnection                   source;
    private final Boolean                        isMaxConnectionsExceeded;
    private final Integer                        maxConnections;
    private final TransportHandshakeErrorContext errorContext;

    public CallContext(ConnectionID connectionId, TransportHandshakeErrorContext errorContext, TCConnection source,
                       Boolean isMaxConnectionsExceeded, Integer maxConnections) {
      this.connectionId = connectionId;
      this.errorContext = errorContext;
      this.source = source;
      this.isMaxConnectionsExceeded = isMaxConnectionsExceeded;
      this.maxConnections = maxConnections;
    }

    public TransportHandshakeErrorContext getErrorContext() {
      return this.errorContext;
    }

    public ConnectionID getConnectionId() {
      return connectionId;
    }

    public Boolean getIsMaxConnectionsExceeded() {
      return isMaxConnectionsExceeded;
    }

    public Integer getMaxConnections() {
      return maxConnections;
    }

    public TCConnection getSource() {
      return source;
    }
  }

}
