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
import com.tc.net.protocol.TCNetworkHeader;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.UUID;

/**
 * 
 */
public class TestProtocolMessage implements OOOProtocolMessage {
  public TCNetworkMessage msg;
  public long             sent;
  public long             ack;
  public boolean          isHandshake          = false;
  public boolean          isHandshakeReplyOk   = false;
  public boolean          isHandshakeReplyFail = false;
  public boolean          isSend               = false;
  public boolean          isAck                = false;
  private boolean         isGoodbye            = false;
  public UUID             sessionId            = UUID.getUUID();

  public TestProtocolMessage(TCNetworkMessage msg, long sent, long ack) {
    this.msg = msg;
    this.sent = sent;
    this.ack = ack;
  }

  public TestProtocolMessage() {
    //
  }

  @Override
  public long getAckSequence() {
    return ack;
  }

  @Override
  public long getSent() {
    return sent;
  }

  @Override
  public boolean isHandshake() {
    return isHandshake;
  }

  @Override
  public boolean isHandshakeReplyOk() {
    return isHandshakeReplyOk;
  }

  @Override
  public boolean isHandshakeReplyFail() {
    return isHandshakeReplyFail;
  }

  @Override
  public boolean isSend() {
    return isSend;
  }

  @Override
  public boolean isAck() {
    return isAck;
  }

  @Override
  public UUID getSessionId() {
    return (sessionId);
  }

  public void setSessionId(UUID id) {
    sessionId = id;
  }

  /*********************************************************************************************************************
   * TCNetworkMessage stuff
   */

  @Override
  public TCNetworkHeader getHeader() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCNetworkMessage getMessagePayload() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCByteBuffer[] getPayload() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TCByteBuffer[] getEntireMessageData() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSealed() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void seal() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getDataLength() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getHeaderLength() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getTotalLength() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void wasSent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSentCallback(Runnable callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Runnable getSentCallback() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void recycle() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isGoodbye() {
    return isGoodbye;
  }

  @Override
  public void reallyDoRecycleOnWrite() {
    //
  }
}
