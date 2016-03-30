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
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.exception.EntityException;

import com.tc.entity.VoltronEntityMessage;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.request.RequestResponseHandler;
import com.tc.text.PrettyPrintable;

import java.util.Set;


/**
 * Maintains the interface for managing server-side entities, from the client side, and also tracks what entities are
 * currently attached from this client, for routing server-originating messages to the correct client-side end-point object.
 * 
 * NOTE:  This interface assumes nothing about locking or access mode semantics so the caller must handle that.
 */
public interface ClientEntityManager extends PrettyPrintable, RequestResponseHandler, ClientHandshakeCallback, InvocationHandler {
  /**
   * Checks if a given entity exists on the server without binding a client instance to it.
   * 
   * @param entityID the entity to check.
   * @param version the version of the implementation to verify
   * @return true if the entity exists.
   */
  // TODO:  Remove version once this check is implemented as a platform intrinsic instead of being a get+release.
  boolean doesEntityExist(EntityID entityID, long version);

  /**
   * Find named entity, returning and end-point to access it.
   * 
   * @param entityDescriptor the entity to look up and the instance making the request.
   * @param closeHook To be passed into the found EntityClientEndpoint for it to call on close or called, directly, if lookup fails.
   * @return The end-point or null if the entity doesn't exist
   */
  public EntityClientEndpoint fetchEntity(EntityDescriptor entityDescriptor, MessageCodec<? extends EntityMessage, ? extends EntityResponse> codec, Runnable closeHook) throws EntityException;

  /**
   * Release the client's reference to the given entityID.
   * 
   * @param entityDescriptor the entity to release and the instance making the request.
   */
  void releaseEntity(EntityDescriptor entityDescriptor) throws EntityException;

  /**
   * Handles a message received from the server. It will hand off the message to the client side entity if it exists.
   * otherwise it'll drop the message on the floor.
   *
   * @param entityDescriptor the entity and instance to receive the message.
   * @param message opaque message
   */
  void handleMessage(EntityDescriptor entityDescriptor, byte[] message);

  InvokeFuture<byte[]> createEntity(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks, byte[] config);
  
  InvokeFuture<byte[]> destroyEntity(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks);

  InvokeFuture<byte[]> reconfigureEntity(EntityID entityID, long version, Set<VoltronEntityMessage.Acks> requestedAcks, byte[] config);
  
  /**
   * Called to retrieve the entityDescriptor, returning its instance configuration, if found.  Note that this call will have
   * the side-effect of adding a reference to this server-side entity, from this client.
   */
  byte[] retrieve(EntityDescriptor entityDescriptor) throws EntityException;

  /**
   * This method is the opposite of "retrieve()":  ask the implementation to tell the remote side we no longer want the entity.
   */
  void release(EntityDescriptor entityDescriptor) throws EntityException;
}
