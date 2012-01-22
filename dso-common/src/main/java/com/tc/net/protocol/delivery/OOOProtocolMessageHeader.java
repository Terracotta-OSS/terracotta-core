/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.AbstractTCNetworkHeader;
import com.tc.net.protocol.TCProtocolException;
import com.tc.util.UUID;

import java.io.UnsupportedEncodingException;

/**
 * Header for once and only once protocol messages.
 */
class OOOProtocolMessageHeader extends AbstractTCNetworkHeader {

  public static final short    TYPE_HANDSHAKE            = 1;
  public static final short    TYPE_HANDSHAKE_REPLY_OK   = 2;
  public static final short    TYPE_HANDSHAKE_REPLY_FAIL = 3;
  public static final short    TYPE_ACK                  = 4;
  public static final short    TYPE_SEND                 = 5;
  public static final short    TYPE_GOODBYE              = 6;

  public static final String[] typeNames                 = new String[] { "N/A", "TYPE_HANDSHAKE",
      "TYPE_HANDSHAKE_REPLY_OK", "TYPE_HANDSHAKE_REPLY_FAIL", "TYPE_ACK", "TYPE_SEND", "TYPE_GOODBYE", };

  public static final short    VERSION                   = 1;

  private static final int     MAGIC_NUM                 = 0xBBBBBBBB;
  private static final int     MAGIC_NUM_OFFSET          = 0;
  private static final int     MAGIC_NUM_LENGTH          = 4;

  private static final int     VERSION_OFFSET            = MAGIC_NUM_OFFSET + MAGIC_NUM_LENGTH;
  private static final int     VERSION_LENGTH            = 1;

  private static final int     TYPE_OFFSET               = VERSION_OFFSET + VERSION_LENGTH;
  private static final int     TYPE_LENGTH               = 1;

  private static final int     SEQUENCE_OFFSET           = TYPE_OFFSET + TYPE_LENGTH;
  private static final int     SEQUENCE_LENGTH           = 8;

  private static final int     SESSION_OFFSET            = SEQUENCE_OFFSET + SEQUENCE_LENGTH;
  private static final int     SESSION_LENGTH            = UUID.SIZE;

  static final int             HEADER_LENGTH;

  static {
    int tmp = MAGIC_NUM_LENGTH + VERSION_LENGTH + TYPE_LENGTH + SEQUENCE_LENGTH + SESSION_LENGTH;
    // This padding is here to ensure that the header is a multiple of four bytes.
    HEADER_LENGTH = (tmp + 3) / 4 * 4;
  }

  OOOProtocolMessageHeader(short version, short type, long sequence, UUID sessionId) {
    super(HEADER_LENGTH, HEADER_LENGTH);
    putValues(version, type, sequence, sessionId);
    try {
      validate();
    } catch (TCProtocolException e) {
      throw new InternalError();
    }
  }

  OOOProtocolMessageHeader(TCByteBuffer buffer) {
    super(buffer, HEADER_LENGTH, HEADER_LENGTH);
  }

  public int getHeaderByteLength() {
    return HEADER_LENGTH;
  }

  private void putValues(short version, short type, long sequence, UUID sessionId) {
    data.putInt(MAGIC_NUM_OFFSET, MAGIC_NUM);
    data.putUbyte(VERSION_OFFSET, version);
    data.putUbyte(TYPE_OFFSET, type);
    data.putLong(SEQUENCE_OFFSET, sequence);
    try {
      data.put(SESSION_OFFSET, sessionId.toString().getBytes("UTF-8"), 0, SESSION_LENGTH);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("OOO SessionID encoding error : " + e);
    }
  }

  protected void setHeaderLength(short headerLength) {
    throw new UnsupportedOperationException("These messages are fixed length.");
  }

  public void validate() throws TCProtocolException {
    int magic = getMagicNumber();
    if (magic != MAGIC_NUM) { throw new TCProtocolException("Bad magic number: " + magic + " != " + MAGIC_NUM); }

    short version = getVersion();
    if (getVersion() != VERSION) { throw new TCProtocolException("Reported version " + version
                                                                 + " is not equal to supported version: " + VERSION); }
    short type = getType();
    if (!isValidType(type)) { throw new TCProtocolException("Unknown message type: " + type); }

    final boolean ack = isAck();
    final boolean ackReq = isHandshake();
    final boolean send = isSend();

    if (ack && (ackReq || send)) { throw new TCProtocolException("Invalid type, ack= " + ack + ", ackRe=" + ackReq
                                                                 + ", send=" + send); }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Type=" + typeNames[getType()]);
    buf.append(" sessId=" + getSession());
    buf.append(" seq=" + getSequence());
    return buf.toString();
  }

  private int getMagicNumber() {
    return data.getInt(MAGIC_NUM_OFFSET);
  }

  private short getVersion() {
    return data.getUbyte(VERSION_OFFSET);
  }

  private short getType() {
    return data.getUbyte(TYPE_OFFSET);
  }

  private boolean isValidType(short type) {
    return type == TYPE_SEND || type == TYPE_HANDSHAKE || type == TYPE_HANDSHAKE_REPLY_OK
           || type == TYPE_HANDSHAKE_REPLY_FAIL || type == TYPE_ACK || type == TYPE_GOODBYE;
  }

  long getSequence() {
    return data.getLong(SEQUENCE_OFFSET);
  }

  UUID getSession() {
    byte[] session = new byte[SESSION_LENGTH];
    data.get(SESSION_OFFSET, session, 0, SESSION_LENGTH);
    try {
      return new UUID(new String(session, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("OOO SessionID encoding error : " + e);
    }
  }

  boolean isHandshake() {
    return getType() == TYPE_HANDSHAKE;
  }

  boolean isHandshakeReplyOk() {
    return getType() == TYPE_HANDSHAKE_REPLY_OK;
  }

  boolean isHandshakeReplyFail() {
    return getType() == TYPE_HANDSHAKE_REPLY_FAIL;
  }

  boolean isAck() {
    return getType() == TYPE_ACK;
  }

  boolean isSend() {
    return getType() == TYPE_SEND;
  }

  boolean isGoodbye() {
    return getType() == TYPE_GOODBYE;
  }

}
