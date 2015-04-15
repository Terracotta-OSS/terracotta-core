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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

class ServerHandshakeMessageResponder extends HandshakeMessageResponderBase {

  private final BlockingQueue<String> synAckErrors = new LinkedBlockingQueue<String>();

  protected ServerHandshakeMessageResponder(BlockingQueue<TransportHandshakeMessage> sentQueue,
                                            BlockingQueue<TransportHandshakeMessage> receivedQueue,
                                            TransportHandshakeMessageFactory messageFactory,
                                            ConnectionID assignedConnectionId, MessageTransportBase transport,
                                            AtomicReference<Throwable> errorRef) {
    super(sentQueue, receivedQueue, messageFactory, assignedConnectionId, transport, errorRef);
  }

  @Override
  public void handleHandshakeMessage(final TransportHandshakeMessage message) {
    try {
      if (message.isSynAck()) {
        Assert.assertNotNull(message.getConnectionId());
        Assert.assertEquals(this.assignedConnectionId, message.getConnectionId());
        final SynAckMessage synAck = (SynAckMessage)message;
        if (synAck.hasErrorContext()) {
          this.synAckErrors.put(synAck.getErrorContext());
        } else {
          TransportHandshakeMessage ack = messageFactory.createAck(this.assignedConnectionId, synAck.getSource());
          this.sendResponseMessage(ack);
        }
      } else {
        Assert.fail("Recieved an unexpected message type: " + message);
      }
    } catch (Exception e) {
      setError(e);
    }
  }

  public boolean wasSynAckReceived(long timeout) throws Exception {
    TransportHandshakeMessage message = this.receivedQueue.poll(timeout, TimeUnit.MILLISECONDS);
    return message != null && message.isSynAck();
  }

  public String waitForSynAckErrorToBeReceived(long timeout) throws InterruptedException {
    return this.synAckErrors.poll(timeout, TimeUnit.MILLISECONDS);
  }

}
