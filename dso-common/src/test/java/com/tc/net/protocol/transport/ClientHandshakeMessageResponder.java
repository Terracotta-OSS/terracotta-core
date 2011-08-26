/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;
import junit.framework.Assert;

class ClientHandshakeMessageResponder extends HandshakeMessageResponderBase {

  protected ClientHandshakeMessageResponder(LinkedQueue sentQueue, LinkedQueue receivedQueue,
                                            TransportHandshakeMessageFactory messageFactory,
                                            ConnectionID assignedConnectionId, MessageTransportBase transport,
                                            SynchronizedRef errorRef) {
    super(sentQueue, receivedQueue, messageFactory, assignedConnectionId, transport, errorRef);
  }

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
      handshake = (TransportHandshakeMessage) receivedQueue.poll(timeout);
      if (handshake == null) return false;
    } while (!(handshake.isAck()));
    return true;
  }

  public boolean waitForSynAckToBeSent(long timeout) throws InterruptedException {
    TransportHandshakeMessage handshake;
    do {
      handshake = (TransportHandshakeMessage) sentQueue.poll(timeout);
      if (handshake == null) return false;
    } while (!handshake.isSynAck());
    return true;
  }
}
