/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  @Override
  public long getAckSequence() {
    return getOOOPHeader().getAckSequence();
  }

  @Override
  public long getSent() {
    return getOOOPHeader().getSequence();
  }

  @Override
  public UUID getSessionId() {
    OOOProtocolMessageHeader header = (OOOProtocolMessageHeader) getHeader();
    return (header.getSession());
  }

  @Override
  public boolean isHandshake() {
    return getOOOPHeader().isHandshake();
  }

  @Override
  public boolean isHandshakeReplyOk() {
    return getOOOPHeader().isHandshakeReplyOk();
  }

  @Override
  public boolean isHandshakeReplyFail() {
    return getOOOPHeader().isHandshakeReplyFail();
  }

  @Override
  public boolean isSend() {
    return getOOOPHeader().isSend();
  }

  @Override
  public boolean isAck() {
    return getOOOPHeader().isAck();
  }

  @Override
  public boolean isGoodbye() {
    return getOOOPHeader().isGoodbye();
  }

  @Override
  public void doRecycleOnWrite() {
    // we are disabling this because on ooo layer knows when it's safe to recycle the message
  }

  @Override
  public void reallyDoRecycleOnWrite() {
    getOOOPHeader().recycle();
    AbstractTCNetworkMessage messagePayLoad = (AbstractTCNetworkMessage) getMessagePayload();
    if (messagePayLoad != null) {
      messagePayLoad.doRecycleOnWrite();
    }
  }
}
