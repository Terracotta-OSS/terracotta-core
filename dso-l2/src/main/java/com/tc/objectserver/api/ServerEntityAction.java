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


/**
 * These "actions" represent the superset of "Request.Type" values.
 * That is, they are an internal representation of what was sent over the wire OR derived for some internal purpose.
 * This is to loosen the coupling between ServerEntityRequest and Request.
 */
public enum ServerEntityAction {
  /**
   * An error value used when the action should not be seen.
   */
  INVALID,
  /**
   * Same as Request.Type.
   */
  FETCH_ENTITY,
  /**
   * Same as Request.Type.
   */
  RELEASE_ENTITY,
  /**
   * Same as Request.Type.
   */
  CREATE_ENTITY,
  /**
   * Same as Request.Type.
   */
  DESTROY_ENTITY,
  /**
   * Same as Request.Type.
   */
  INVOKE_ACTION,
  /**
   * Ask an entity to synchronize itself to a passive.
   */
  REQUEST_SYNC_ENTITY,
  /**
   * An internally-created action to communicate that an entity should be loaded from its existing disk state.
   */
  LOAD_EXISTING_ENTITY,
  /**
   * Reload the active entity with the new supplied configuration.
   */
  RECONFIGURE_ENTITY,  
  // ***** Messages specific to received passive synchronization data below this point *****
  /**
   * Messages related to the synchronization of a specific entity instance follow.
   */
  RECEIVE_SYNC_ENTITY_START,
  /**
   * Messages related to the synchronization of a specific entity instance are now done.
   */
  RECEIVE_SYNC_ENTITY_END,
  /**
   * Messages related to the synchronization of a specific entity concurrency key follow.
   */
  RECEIVE_SYNC_ENTITY_KEY_START,
  /**
   * Messages related to the synchronization of a specific entity concurrency key are now done.
   */
  RECEIVE_SYNC_ENTITY_KEY_END,
  /**
   * A synchronized state message on a specific concurrency key within a specific entity instance.
   */
  RECEIVE_SYNC_PAYLOAD,
  // ***** END: Messages specific to received passive synchronization data *****
  /**
   * An action which should never be replicated, just used to synchronize on the flush of local executor queues.
   */
  LOCAL_FLUSH,
  /**
   * An action which should never be replicated, just used to clean up a deleted entity, after a pipeline flush.
   */
  LOCAL_FLUSH_AND_DELETE,
  /**
   * An action which should never be replicated, just used to start a synchronization, after a pipeline flush.
   */
  LOCAL_FLUSH_AND_SYNC,
  /**
   * Used in message replication:  we often don't want to replicate the contents of the message or its intent, just
   *  information which might be required to correctly order re-sends, after fail-over.
   */
  ORDER_PLACEHOLDER_ONLY;
}
