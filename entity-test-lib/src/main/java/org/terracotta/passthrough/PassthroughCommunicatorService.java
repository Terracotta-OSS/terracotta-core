/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;

import java.util.concurrent.Future;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.passthrough.PassthroughBuiltInServiceProvider.DeferredEntityContainer;


/**
 * The implementation of the communicator service which is normally built-in to the platform.
 * This is used, by server-side entities, to send messages back to specific client-side instances.
 * TODO:  we currently need to determine how to handle the synchronous send.
 */
public class PassthroughCommunicatorService implements ClientCommunicator {
  @SuppressWarnings("unused")
  // Currently unused but in place for a later change.
  private final DeferredEntityContainer container;

  public PassthroughCommunicatorService(DeferredEntityContainer container) {
    // Nobody should be able to request the communicator if they are a non-entity consumer (null container).
    Assert.assertTrue(null != container);
    this.container = container;
  }

  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, byte[] payload) {
    prepareAndSendMessage(clientDescriptor, payload);
  }

  @Override
  public Future<Void> send(ClientDescriptor clientDescriptor, byte[] payload) {
    return prepareAndSendMessage(clientDescriptor, payload);
  }

  private Future<Void> prepareAndSendMessage(ClientDescriptor clientDescriptor, byte[] payload) {
    PassthroughClientDescriptor rawDescriptor = (PassthroughClientDescriptor) clientDescriptor;
    PassthroughConnection connection = rawDescriptor.sender;
    long clientInstanceID = rawDescriptor.clientInstanceID;
    Future<Void> waiter = connection.createClientResponseFuture();
    PassthroughMessage message = PassthroughMessageCodec.createMessageToClient(clientInstanceID, payload);
    connection.sendMessageToClient(rawDescriptor.server, message.asSerializedBytes());
    return waiter;
  }
}
