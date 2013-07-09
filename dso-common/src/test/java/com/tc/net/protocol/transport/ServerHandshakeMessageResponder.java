/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
