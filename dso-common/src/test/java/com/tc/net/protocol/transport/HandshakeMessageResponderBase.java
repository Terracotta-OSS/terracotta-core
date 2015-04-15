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
