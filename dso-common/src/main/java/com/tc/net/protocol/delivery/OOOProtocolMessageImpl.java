/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.AbstractTCNetworkMessage;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.UUID;

class OOOProtocolMessageImpl extends AbstractTCNetworkMessage implements OOOProtocolMessage {

  /**
   * Create a header-only message (no payload). Useful for ack and ack request messages.
   */
  OOOProtocolMessageImpl(OOOProtocolMessageHeader header) {
    super(header, true);
  }

  /**
   * Create a message with the given payload from the network. Useful for propogating messages up the network stack.
   */
  OOOProtocolMessageImpl(OOOProtocolMessageHeader header, TCByteBuffer[] payload) {
    super(header, payload);
  }

  /**
   * Create a message with the given TCNetworkMessage payload. Useful for propogating messages down the network stack.
   */
  OOOProtocolMessageImpl(OOOProtocolMessageHeader header, TCNetworkMessage msgPayload) {
    super(header, msgPayload);
  }

  private OOOProtocolMessageHeader getOOOPHeader() {
    return (OOOProtocolMessageHeader) getHeader();
  }

  public long getAckSequence() {
    return getOOOPHeader().getSequence();
  }

  public long getSent() {
    return getOOOPHeader().getSequence();
  }

  public UUID getSessionId() {
    OOOProtocolMessageHeader header = (OOOProtocolMessageHeader) getHeader();
    return (header.getSession());
  }

  public boolean isHandshake() {
    return getOOOPHeader().isHandshake();
  }

  public boolean isHandshakeReplyOk() {
    return getOOOPHeader().isHandshakeReplyOk();
  }

  public boolean isHandshakeReplyFail() {
    return getOOOPHeader().isHandshakeReplyFail();
  }

  public boolean isSend() {
    return getOOOPHeader().isSend();
  }

  public boolean isAck() {
    return getOOOPHeader().isAck();
  }

  public boolean isGoodbye() {
    return getOOOPHeader().isGoodbye();
  }

  public void doRecycleOnWrite() {
    // we are disabling this because on ooo layer knows when it's safe to recycle the message
  }

  public void reallyDoRecycleOnWrite() {
    getOOOPHeader().recycle();
    AbstractTCNetworkMessage messagePayLoad = (AbstractTCNetworkMessage) getMessagePayload();
    if (messagePayLoad != null) {
      messagePayLoad.doRecycleOnWrite();
    }
  }
}