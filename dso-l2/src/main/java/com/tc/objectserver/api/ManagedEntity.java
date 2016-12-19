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
package com.tc.objectserver.api;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodec;

import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.EntityID;
import com.tc.objectserver.entity.MessagePayload;
import com.tc.objectserver.entity.SimpleCompletion;
import com.tc.objectserver.handler.RetirementManager;
import java.util.function.Consumer;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import org.terracotta.entity.StateDumpable;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityUserException;


/**
 * Contains a managed entity or holds a place for a specific entity yet-to-be-created.
 * The ProcessTransactionHandler passes requests into this to be applied to the underlying entity.
 * Additionally, client-entity connections are rebuilt, after reconnect, using this interface.
 */
public interface ManagedEntity extends StateDumpable {
  public final static int UNDELETABLE_ENTITY = -1;
  
  public EntityID getID();
  
  public long getVersion();
 /** 
  * Schedules the request with the entity on the execution queue.
  * 
  * @param request translated request for execution on the server
  * @param entityMessage The message instance, if it was generated internally (the extendedData is still expected)
  * @param extendedData payload of the invoke
  * @param defaultKey default concurrency key if no concurrency strategy is installed
  * @throws EntityUserException A state-safe exception (MessageCodecException) was encountered while setting up the invoke.
  */ 
  SimpleCompletion addRequestMessage(ServerEntityRequest request, MessagePayload data, Consumer<byte[]> completion, Consumer<EntityException> exception);
    
  /**
   * Called to handle the reconnect for a specific client instance living on a specific node.
   * This is called after restart or fail-over to re-associate a formerly connected client with its server-side entities.
   * Note that this call is made BEFORE any re-sent transactions are issued to the entity.
   * 
   * @param clientID The client node involved in the reconnect
   * @param clientDescriptor The specific instance on that client which is requesting to reconnect
   * @param extendedReconnectData Free-formed data sent by the client to help restore the in-memory state of the entity
   */
  public void reconnectClient(ClientID clientID, ClientDescriptor clientDescriptor, byte[] extendedReconnectData);
  /**
   * Called to sync an entity.  Caller initiates sync of an entity through this method.  
   * 
   * @param passive target passive
   */
  void sync(NodeID passive);
  
  void startSync();
  
  void loadEntity(byte[] configuration) throws ConfigurationException;
  
  void promoteEntity() throws ConfigurationException;
    
  boolean isDestroyed();
  
  boolean isActive();
  
  boolean isRemoveable();
  
  boolean clearQueue();
  
  void resetReferences(int count);

  /**
   * Used when an external component (such as CommunicatorService) needs to translate to/from something specific to this
   * entity's dialect.
   * 
   * @return The codec which can translate to/from this entity's message dialect.
   */
  MessageCodec<? extends EntityMessage, ? extends EntityResponse> getCodec();
  /**
   * Of specific interest to the EntityMessengerService since it may need to install dependencies between messages to this
   * entity.
   * 
   * @return The entity's local RetirementManager instance.
   */
  public RetirementManager getRetirementManager();
}
