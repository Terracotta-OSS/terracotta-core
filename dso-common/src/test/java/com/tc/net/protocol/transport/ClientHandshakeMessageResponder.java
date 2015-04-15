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
package com.tc.net.protocol.transport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

class ClientHandshakeMessageResponder extends HandshakeMessageResponderBase {

  protected ClientHandshakeMessageResponder(BlockingQueue<TransportHandshakeMessage> sentQueue,
                                            BlockingQueue<TransportHandshakeMessage> receivedQueue,
                                            TransportHandshakeMessageFactory messageFactory,
                                            ConnectionID assignedConnectionId, MessageTransportBase transport,
                                            AtomicReference<Throwable> errorRef) {
    super(sentQueue, receivedQueue, messageFactory, assignedConnectionId, transport, errorRef);
  }

  @Override
  public void handleHandshakeMessage(TransportHandshakeMessage message) {
    if (message.isSyn()) {

      Assert.assertNotNull(message.getConnectionId());
      sendResponseMessage(messageFactory.createSynAck(this.assignedConnectionId, message.getSource(), false, -1,
                                                      TransportHandshakeMessage.NO_CALLBACK_PORT));
    } else if (message.isAck()) {
      // nothing to do.
    } else {
      Assert.fail("Bogus message received: " + message);
    }
  }

  public boolean waitForAckToBeReceived(long timeout) throws InterruptedException {
    TransportHandshakeMessage handshake;
    do {
      handshake = receivedQueue.poll(timeout, TimeUnit.MILLISECONDS);
      if (handshake == null) return false;
    } while (!(handshake.isAck()));
    return true;
  }

  public boolean waitForSynAckToBeSent(long timeout) throws InterruptedException {
    TransportHandshakeMessage handshake;
    do {
      handshake = sentQueue.poll(timeout, TimeUnit.MILLISECONDS);
      if (handshake == null) return false;
    } while (!handshake.isSynAck());
    return true;
  }
}
