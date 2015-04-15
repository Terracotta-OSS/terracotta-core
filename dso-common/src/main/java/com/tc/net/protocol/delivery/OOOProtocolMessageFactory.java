/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.delivery;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.UUID;

public class OOOProtocolMessageFactory {

  public OOOProtocolMessage createNewHandshakeMessage(UUID sessionId, long ack) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_HANDSHAKE, ack,
                                                                   sessionId));
  }

  public OOOProtocolMessage createNewAckMessage(UUID sessionId, long ackSequence) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_ACK, 0, ackSequence,
                                                                   sessionId));
  }

  public OOOProtocolMessage createNewSendMessage(UUID sessionId, long sequence, long ackSequence,
                                                 TCNetworkMessage payload) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_SEND, sequence, ackSequence,
                                                                   sessionId), payload);
  }

  public OOOProtocolMessage createNewMessage(OOOProtocolMessageHeader header, TCByteBuffer[] data) {
    return new OOOProtocolMessageImpl(header, data);
  }

  public OOOProtocolMessage createNewGoodbyeMessage(UUID sessionId) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_GOODBYE, 0, sessionId));
  }

  public OOOProtocolMessage createNewHandshakeReplyOkMessage(UUID sessionId, long sequence) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_HANDSHAKE_REPLY_OK,
                                                                   sequence, sessionId));
  }

  public OOOProtocolMessage createNewHandshakeReplyFailMessage(UUID sessionId, long sequence) {
    return new OOOProtocolMessageImpl(new OOOProtocolMessageHeader(OOOProtocolMessageHeader.VERSION,
                                                                   OOOProtocolMessageHeader.TYPE_HANDSHAKE_REPLY_FAIL,
                                                                   sequence, sessionId));
  }
}
