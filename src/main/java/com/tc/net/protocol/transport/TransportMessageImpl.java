/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
   * VERSION_3: Transport Handshake Message Version for Terracotta = 3.6 (Ulloa)
   * VERSION_4: Transport Handshake Message Version for Terracotta = 3.7 (Gladstone)
   * VERSION_5: Transport Handshake Message Version for Terracotta = 4.1 (Wawona)
   * VERSION: Current Version for Transport Handshake Messages
   */
  static final byte          VERSION_1  = 1;
  static final byte          VERSION_2  = 2;
  static final byte          VERSION_3  = 3;
  static final byte          VERSION_4  = 4;
  static final byte          VERSION_5  = 5;
  static final byte          VERSION    = VERSION_5;


  static final byte          SYN        = 1;
  static final byte          ACK        = 2;
  static final byte          SYN_ACK    = 3;
  static final byte          PING       = 4;
  static final byte          PING_REPLY = 5;
  static final byte          TIME_CHECK = 6;

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
  private final long         timestamp;

  TransportMessageImpl(TCConnection source, TCNetworkHeader header, TCByteBuffer[] payload) throws TCProtocolException {
    super(source, header, payload);

    try {
      TCByteBufferInputStream in = new TCByteBufferInputStream(payload);
      this.version = in.readByte();

      if (version != VERSION) { throw new TCProtocolException("Version Mismatch for Transport Message Handshake: " + version + " != " + VERSION); }

      this.type = in.readByte();

      this.connectionId = ConnectionID.readFrom(in);

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
      this.timestamp = (type == TIME_CHECK) ? in.readLong() : -1;
    } catch (TCProtocolException e) {
      throw e;
    } catch (Exception e) {
      throw new TCProtocolException("Exception reading data: ", e);
    }
  }

  @Override
  public void doRecycleOnWrite() {
    recycle();
  }

  @Override
  protected String describePayload() {
    return "type: " + typeToString() + ", connectionId: " + connectionId + ", timestamp: " + timestamp
           + ", errorContext " + errorContext + "\n";
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
      case TIME_CHECK:
        return "TIME_CHECK";
      default:
        return "UNKNOWN";
    }
  }

  @Override
  public ConnectionID getConnectionId() {
    return this.connectionId;
  }

  @Override
  public boolean hasErrorContext() {
    return this.hasErrorContext;
  }

  @Override
  public String getErrorContext() {
    Assert.eval(hasErrorContext());
    return this.errorContext;
  }

  @Override
  public short getErrorType() {
    Assert.eval(hasErrorContext());
    return this.errorType;
  }

  @Override
  public boolean isPing() {
    return type == PING;
  }

  @Override
  public boolean isPingReply() {
    return type == PING_REPLY;
  }

  @Override
  public long getTime() {
    return this.timestamp;
  }

  @Override
  public boolean isSynAck() {
    return type == SYN_ACK;
  }

  @Override
  public boolean isSyn() {
    return type == SYN;
  }

  @Override
  public boolean isAck() {
    return type == ACK;
  }

  @Override
  public boolean isTimeCheck() {
    return type == TIME_CHECK;
  }

  @Override
  public boolean isMaxConnectionsExceeded() {
    return this.isMaxConnectionsExceeded;
  }

  @Override
  public int getMaxConnections() {
    return this.maxConnections;
  }

  @Override
  public short getStackLayerFlags() {
    return this.stackLayerFlags;
  }

  @Override
  public int getCallbackPort() {
    return this.callbackPort;
  }

}
