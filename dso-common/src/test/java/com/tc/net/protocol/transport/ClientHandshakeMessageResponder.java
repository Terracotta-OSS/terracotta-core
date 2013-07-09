/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
