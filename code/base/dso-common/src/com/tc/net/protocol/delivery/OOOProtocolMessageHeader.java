/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.AbstractTCNetworkHeader;
import com.tc.net.protocol.TCProtocolException;

/**
 * Header for once and only once protocol messages.
 */
class OOOProtocolMessageHeader extends AbstractTCNetworkHeader {

  private static final short TYPE_ACK_REQUEST = 1;
  private static final short TYPE_ACK         = 2;
  private static final short TYPE_SEND        = 3;
  private static final short TYPE_GOODBYE     = 4;

  private static final short VERSION          = 1;

  private static final int   MAGIC_NUM        = 0xBBBBBBBB;
  private static final int   MAGIC_NUM_OFFSET = 0;
  private static final int   MAGIC_NUM_LENGTH = 4;

  private static final int   VERSION_OFFSET   = MAGIC_NUM_OFFSET + MAGIC_NUM_LENGTH;
  private static final int   VERSION_LENGTH   = 1;

  private static final int   TYPE_OFFSET      = VERSION_OFFSET + VERSION_LENGTH;
  private static final int   TYPE_LENGTH      = 1;

  private static final int   SEQUENCE_OFFSET  = TYPE_OFFSET + TYPE_LENGTH;
  private static final int   SEQUENCE_LENGTH  = 8;

  static final int           HEADER_LENGTH;

  static {
    int tmp = MAGIC_NUM_LENGTH + VERSION_LENGTH + TYPE_LENGTH + SEQUENCE_LENGTH;
    // This padding is here to ensure that the header is a multiple of four bytes.
    HEADER_LENGTH = tmp + (tmp % 4);
  }

  private OOOProtocolMessageHeader(short version, short type, long sequence) {
    super(HEADER_LENGTH, HEADER_LENGTH);
    putValues(version, type, sequence);
    try {
      validate();
    } catch (TCProtocolException e) {
      throw new InternalError();
    }
  }

  private OOOProtocolMessageHeader(TCByteBuffer buffer) {
    super(buffer, HEADER_LENGTH, HEADER_LENGTH);
  }

  public int getHeaderByteLength() {
    return HEADER_LENGTH;
  }

  private void putValues(short version, short type, long sequence) {
    data.putInt(MAGIC_NUM_OFFSET, MAGIC_NUM);
    data.putUbyte(VERSION_OFFSET, version);
    data.putUbyte(TYPE_OFFSET, type);
    data.putLong(SEQUENCE_OFFSET, sequence);
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
    final boolean ackReq = isAckRequest();
    final boolean send = isSend();

    if (ack && (ackReq || send)) { throw new TCProtocolException("Invalid type, ack= " + ack + ", ackRe=" + ackReq
                                                                 + ", send=" + send); }
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();

    buf.append("valid: ");
    try {
      validate();
      buf.append("true");
    } catch (TCProtocolException e) {
      buf.append("false (").append(e.getMessage()).append(")");
    }
    buf.append(", ");

    buf.append("type: ");
    if (isAck()) {
      buf.append("ACK ").append(getSequence());
    } else if (isAckRequest()) {
      buf.append("ACK REQ");
    } else if (isSend()) {
      buf.append("SEND " + getSequence());
    } else {
      buf.append("UNKNOWN");
    }
    buf.append('\n');

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
    return type == TYPE_SEND || type == TYPE_ACK_REQUEST || type == TYPE_ACK || type == TYPE_GOODBYE;
  }

  long getSequence() {
    return data.getLong(SEQUENCE_OFFSET);
  }

  boolean isAckRequest() {
    return getType() == TYPE_ACK_REQUEST;
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

  static class ProtocolMessageHeaderFactory {

    /**
     * Use to create new headers for sending ack request messages.
     */
    OOOProtocolMessageHeader createNewAckRequest() {
      return new OOOProtocolMessageHeader(VERSION, TYPE_ACK_REQUEST, 0);
    }

    /**
     * Use to create new headers for sending ack messages.
     */
    OOOProtocolMessageHeader createNewAck(long sequence) {
      return new OOOProtocolMessageHeader(VERSION, TYPE_ACK, sequence);
    }

    /**
     * Use to create new headers for sending wrapped messages.
     */
    OOOProtocolMessageHeader createNewSend(long sequence) {
      return new OOOProtocolMessageHeader(VERSION, TYPE_SEND, sequence);
    }

    /**
     * Use with buffers read off of the network.
     */
    OOOProtocolMessageHeader createNewHeader(TCByteBuffer buffer) {
      return new OOOProtocolMessageHeader(buffer.duplicate().limit(OOOProtocolMessageHeader.HEADER_LENGTH));
    }

    public OOOProtocolMessageHeader createNewGoodbye() {
      return new OOOProtocolMessageHeader(VERSION, TYPE_GOODBYE, 0);
    }
  }
}