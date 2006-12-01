/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCProtocolException;
import com.tc.util.Assert;

import java.io.IOException;

class TransportHandshakeMessageImpl extends WireProtocolMessageImpl implements SynMessage, SynAckMessage, AckMessage {
  static final byte          VERSION_1 = 1;

  static final byte          SYN       = 1;
  static final byte          ACK       = 2;
  static final byte          SYN_ACK   = 3;

  private final byte         version;
  private final byte         type;
  private final ConnectionID connectionId;
  private final String       errorContext;
  private final boolean      hasErrorContext;
  private final int          maxConnections;
  private final boolean      isMaxConnectionsExceeded;

  TransportHandshakeMessageImpl(TCConnection source, TCNetworkHeader header, TCByteBuffer[] payload)
      throws TCProtocolException {
    super(source, header, payload);

    try {
      TCByteBufferInputStream in = new TCByteBufferInputStream(payload);
      this.version = in.readByte();

      if (version != VERSION_1) { throw new TCProtocolException("Bad Version: " + version + " != " + VERSION_1); }

      this.type = in.readByte();

      try {
        this.connectionId = ConnectionID.parse(in.readString());
      } catch (InvalidConnectionIDException e) {
        throw new TCProtocolException(e);
      }

      this.isMaxConnectionsExceeded = in.readBoolean();
      this.maxConnections = in.readInt();
      this.hasErrorContext = in.readBoolean();

      if (this.hasErrorContext) {
        this.errorContext = in.readString();
      } else {
        this.errorContext = null;
      }

    } catch (IOException e) {
      throw new TCProtocolException("IOException reading data: " + e.getMessage());
    }
  }

  public void doRecycleOnWrite() {
    recycle();
  }

  protected String describePayload() {
    return "type: " + typeToString() + ", connectionId: " + connectionId + ", errorContext " + errorContext + "\n";
  }

  private String typeToString() {
    switch (type) {
      case SYN:
        return "SYN";
      case ACK:
        return "ACK";
      case SYN_ACK:
        return "SYN_ACK";
      default:
        return "UNKNOWN";
    }
  }

  public ConnectionID getConnectionId() {
    return this.connectionId;
  }

  public boolean hasErrorContext() {
    return this.hasErrorContext;
  }

  public String getErrorContext() {
    Assert.eval(hasErrorContext());
    return this.errorContext;
  }

  public boolean isSynAck() {
    return type == SYN_ACK;
  }

  public boolean isSyn() {
    return type == SYN;
  }

  public boolean isAck() {
    return type == ACK;
  }

  public boolean hasDefaultConnectionId() {
    Assert.assertNotNull(connectionId);
    return this.connectionId.isNull();
  }

  public boolean isMaxConnectionsExceeded() {
    return this.isMaxConnectionsExceeded;
  }

  public int getMaxConnections() {
    return this.maxConnections;
  }

}