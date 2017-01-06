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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.exception.EntityException;

import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.request.RequestResponseHandler;
import com.tc.text.PrettyPrintable;



/**
 * Maintains the interface for managing server-side entities, from the client side, and also tracks what entities are
 * currently attached from this client, for routing server-originating messages to the correct client-side end-point object.
 * 
 * NOTE:  This interface assumes nothing about locking or access mode semantics so the caller must handle that.
 */
public interface ClientEntityManager extends PrettyPrintable, RequestResponseHandler, ClientHandshakeCallback, InvocationHandler {

  /**
   * Find named entity, returning and end-point to access it.
   * 
   * @param entityDescriptor the entity to look up and the instance making the request.
   * @param closeHook To be passed into the found EntityClientEndpoint for it to call on close or called, directly, if lookup fails.
   * @return The end-point or null if the entity doesn't exist
   */
  public EntityClientEndpoint fetchEntity(EntityDescriptor entityDescriptor, MessageCodec<? extends EntityMessage, ? extends EntityResponse> codec, Runnable closeHook) throws EntityException;

  /**
   * Handles a message received from the server. It will hand off the message to the client side entity if it exists.
   * otherwise it'll drop the message on the floor.
   *
   * @param entityDescriptor the entity and instance to receive the message.
   * @param message opaque message
   */
  void handleMessage(EntityDescriptor entityDescriptor, byte[] message);

  byte[] createEntity(EntityID entityID, long version, byte[] config) throws EntityException;
  
  boolean destroyEntity(EntityID entityID, long version) throws EntityException;

  byte[] reconfigureEntity(EntityID entityID, long version, byte[] config) throws EntityException;
}
