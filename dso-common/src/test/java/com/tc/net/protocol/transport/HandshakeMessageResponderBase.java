/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.NetworkMessageSink;
import com.tc.net.protocol.TCNetworkMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

abstract class HandshakeMessageResponderBase implements NetworkMessageSink, HandshakeMessageResponder {
  protected final ConnectionID                     assignedConnectionId;
  private final MessageTransportBase               transport;
  protected final TransportHandshakeMessageFactory messageFactory;
  protected BlockingQueue<TransportHandshakeMessage> sentQueue;
  protected BlockingQueue<TransportHandshakeMessage> receivedQueue;
  private final AtomicReference<Throwable>           errorRef;

  protected HandshakeMessageResponderBase(BlockingQueue<TransportHandshakeMessage> sentQueue,
                                          BlockingQueue<TransportHandshakeMessage> receivedQueue,
                                          TransportHandshakeMessageFactory messageFactory,
                                          ConnectionID assignedConnectionId, MessageTransportBase transport,
                                          AtomicReference<Throwable> errorRef) {
    super();
    this.sentQueue = sentQueue;
    this.receivedQueue = receivedQueue;
    this.messageFactory = messageFactory;
    this.assignedConnectionId = assignedConnectionId;
    this.transport = transport;
    this.errorRef = errorRef;
  }

  @Override
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
      @Override
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
