/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
  public EntityClientEndpoint fetchEntity(EntityID entity, long version, ClientInstanceID instance, MessageCodec<? extends EntityMessage, ? extends EntityResponse> codec) throws EntityException;

  byte[] createEntity(EntityID entityID, long version, byte[] config) throws EntityException;
  
  boolean destroyEntity(EntityID entityID, long version) throws EntityException;

  byte[] reconfigureEntity(EntityID entityID, long version, byte[] config) throws EntityException;

  boolean isValid();
}
