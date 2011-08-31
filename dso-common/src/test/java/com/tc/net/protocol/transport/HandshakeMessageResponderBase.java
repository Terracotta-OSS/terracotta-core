/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.net.protocol.NetworkMessageSink;
import com.tc.net.protocol.TCNetworkMessage;

import junit.framework.Assert;

abstract class HandshakeMessageResponderBase implements NetworkMessageSink, HandshakeMessageResponder {
  protected final ConnectionID                     assignedConnectionId;
  private final MessageTransportBase               transport;
  protected final TransportHandshakeMessageFactory messageFactory;
  protected LinkedQueue                            sentQueue;
  protected LinkedQueue                            receivedQueue;
  private final SynchronizedRef                    errorRef;

  protected HandshakeMessageResponderBase(LinkedQueue sentQueue, LinkedQueue receivedQueue,
                                          TransportHandshakeMessageFactory messageFactory,
                                          ConnectionID assignedConnectionId, MessageTransportBase transport,
                                          SynchronizedRef errorRef) {
    super();
    this.sentQueue = sentQueue;
    this.receivedQueue = receivedQueue;
    this.messageFactory = messageFactory;
    this.assignedConnectionId = assignedConnectionId;
    this.transport = transport;
    this.errorRef = errorRef;
  }

  public void putMessage(TCNetworkMessage msg) {
    Assert.assertTrue(msg instanceof TransportHandshakeMessage);
    TransportHandshakeMessage message = (TransportHandshakeMessage) msg;

    try {
      this.receivedQueue.put(message);
      handleHandshakeMessage(message);
    } catch (InterruptedException e) {
      setError(e);
    }
  }

  protected void setError(Exception e) {
    e.printStackTrace();
    errorRef.set(e);
  }

  protected void sendResponseMessage(final TransportHandshakeMessage responseMessage) {
    new Thread(new Runnable() {
      public void run() {
        try {
          sentQueue.put(responseMessage);
          transport.receiveTransportMessage(responseMessage);
        } catch (Exception e) {
          setError(e);
        }
      }
    }).start();
  }
}