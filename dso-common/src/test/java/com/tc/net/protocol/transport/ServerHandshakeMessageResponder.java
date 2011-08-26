/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.net.protocol.transport.TransportHandshakeMessage;

import junit.framework.Assert;

class ServerHandshakeMessageResponder extends HandshakeMessageResponderBase {

  private LinkedQueue synAckErrors = new LinkedQueue();

  protected ServerHandshakeMessageResponder(LinkedQueue sentQueue, LinkedQueue receivedQueue,
                                            TransportHandshakeMessageFactory messageFactory,
                                            ConnectionID assignedConnectionId, MessageTransportBase transport,
                                            SynchronizedRef errorRef) {
    super(sentQueue, receivedQueue, messageFactory, assignedConnectionId, transport, errorRef);
  }

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
    TransportHandshakeMessage message = (TransportHandshakeMessage) this.receivedQueue.poll(timeout);
    return message != null && message.isSynAck();
  }

  public String waitForSynAckErrorToBeReceived(long timeout) throws InterruptedException {
    return (String) this.synAckErrors.poll(timeout);
  }

}
