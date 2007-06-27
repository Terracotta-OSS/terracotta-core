/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.TCNetworkMessage;

public class OOOProtocolMessageFactory {

  public OOOProtocolMessage createNewHandshakeMessage(short sessionId, long ack) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_HANDSHAKE, ack,
                                                                   sessionId));
  }

  public OOOProtocolMessage createNewAckMessage(short sessionId, long sequence) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_ACK, sequence,
                                                                   sessionId));
  }

  public OOOProtocolMessage createNewSendMessage(short sessionId, long sequence, TCNetworkMessage payload) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_SEND, sequence,
                                                                   sessionId), payload);
  }

  public OOOProtocolMessage createNewMessage(OOOProtocolMessageHeader header, TCByteBuffer[] data) {
    return new OOOProtocolMessageImpl(header, data);
  }

  public OOOProtocolMessage createNewGoodbyeMessage(short sessionId) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_GOODBYE, 0, sessionId));
  }

  public OOOProtocolMessage createNewHandshakeReplyOkMessage(short sessionId, long sequence) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_HANDSHAKE_REPLY_OK,
                                                                   sequence, sessionId));
  }

  public OOOProtocolMessage createNewHandshakeReplyFailMessage(short sessionId, long sequence) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_HANDSHAKE_REPLY_FAIL,
                                                                   sequence, sessionId));
  }
}
