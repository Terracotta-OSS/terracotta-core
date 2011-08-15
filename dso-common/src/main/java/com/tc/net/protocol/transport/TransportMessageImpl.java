/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCProtocolException;
import com.tc.util.Assert;

import java.io.IOException;

class TransportMessageImpl extends WireProtocolMessageImpl implements SynMessage, SynAckMessage, AckMessage,
    HealthCheckerProbeMessage {
  
  /**
   * VERSION_1: Transport Handshake Message Version for Terracotta <= 2.5
   * VERSION_2: Transport Handshake Message Version for Terracotta = 2.6
   * VERSION_2: Transport Handshake Message Version for Terracotta = 3.6 (Ulloa)
   * VERSION: Current Version for Transport Handshake Messages
   */
  static final byte          VERSION_1  = 1;
  static final byte          VERSION_2  = 2;
  static final byte          VERSION_3  = 3;
  static final byte          VERSION    = VERSION_3;


  static final byte          SYN        = 1;
  static final byte          ACK        = 2;
  static final byte          SYN_ACK    = 3;
  static final byte          PING       = 4;
  static final byte          PING_REPLY = 5;

  private final byte         version;
  private final byte         type;
  private final ConnectionID connectionId;
  private final String       errorContext;
  private final boolean      hasErrorContext;
  private final int          maxConnections;
  private final boolean      isMaxConnectionsExceeded;
  private final short        stackLayerFlags;
  private final short        errorType;
  private final int          callbackPort;

  TransportMessageImpl(TCConnection source, TCNetworkHeader header, TCByteBuffer[] payload) throws TCProtocolException {
    super(source, header, payload);

    try {
      TCByteBufferInputStream in = new TCByteBufferInputStream(payload);
      this.version = in.readByte();

      if (version != VERSION) { throw new TCProtocolException("Version Mismatch for Transport Message Handshake: " + version + " != " + VERSION); }

      this.type = in.readByte();

      try {
        this.connectionId = ConnectionID.parse(in.readString());
      } catch (InvalidConnectionIDException e) {
        throw new TCProtocolException(e);
      }

      this.isMaxConnectionsExceeded = in.readBoolean();
      this.maxConnections = in.readInt();
      this.stackLayerFlags = in.readShort();
      this.callbackPort = in.readInt();
      this.hasErrorContext = in.readBoolean();

      if (this.hasErrorContext) {
        this.errorType = in.readShort();
        this.errorContext = in.readString();
      } else {
        this.errorType = TransportHandshakeError.ERROR_NONE;
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
      case PING:
        return "PING";
      case PING_REPLY:
        return "PING_REPLY";
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

  public short getErrorType() {
    Assert.eval(hasErrorContext());
    return this.errorType;
  }

  public boolean isPing() {
    return type == PING;
  }

  public boolean isPingReply() {
    return type == PING_REPLY;
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

  public short getStackLayerFlags() {
    return this.stackLayerFlags;
  }

  public int getCallbackPort() {
    return this.callbackPort;
  }

}