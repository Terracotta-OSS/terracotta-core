/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.passthrough;

import java.util.concurrent.Future;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.passthrough.PassthroughImplementationProvidedServiceProvider.DeferredEntityContainer;


/**
 * The implementation of the communicator service which is normally built-in to the platform.
 * This is used, by server-side entities, to send messages back to specific client-side instances.
 * TODO:  we currently need to determine how to handle the synchronous send.
 */
public class PassthroughCommunicatorService implements ClientCommunicator {
  private final DeferredEntityContainer container;

  public PassthroughCommunicatorService(DeferredEntityContainer container) {
    // Nobody should be able to request the communicator if they are a non-entity consumer (null container).
    Assert.assertTrue(null != container);
    this.container = container;
  }

  @Override
  public void closeClientConnection(ClientDescriptor clientDescriptor) {
    PassthroughClientDescriptor rawDescriptor = (PassthroughClientDescriptor) clientDescriptor;
    PassthroughConnection connection = rawDescriptor.sender;
    connection.close();
  }

  @Override
  public void sendNoResponse(ClientDescriptor clientDescriptor, EntityResponse message) throws MessageCodecException {
    prepareAndSendMessage(clientDescriptor, message);
  }

  private Future<Void> prepareAndSendMessage(ClientDescriptor clientDescriptor, EntityResponse entityMessage) throws MessageCodecException {
    PassthroughClientDescriptor rawDescriptor = (PassthroughClientDescriptor) clientDescriptor;
    PassthroughConnection connection = rawDescriptor.sender;
    long clientInstanceID = rawDescriptor.clientInstanceID;
    Future<Void> waiter = connection.createClientResponseFuture();
    
    // We know that the entity better exist, by this point, to use the service.
    CommonServerEntity<?, ?> entity = this.container.getEntity();
    Assert.assertTrue(null != entity);
    byte[] payload = serialize(this.container.codec, entityMessage);
    PassthroughMessage message = PassthroughMessageCodec.createMessageToClient(clientInstanceID, payload);
    connection.sendMessageToClient(rawDescriptor.server, message.asSerializedBytes());
    return waiter;
  }

  @SuppressWarnings("unchecked")
  private <R extends EntityResponse> byte[] serialize(MessageCodec<?, R> codec, EntityResponse message) throws MessageCodecException {
    // Cast should be safe as message and codec are from the same implementation.
    return codec.encodeResponse((R)message);
  }
}
