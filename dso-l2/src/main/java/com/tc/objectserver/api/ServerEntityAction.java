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
  NOOP,
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
  DOES_EXIST,
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
   * An internally-created action to communicate that an entity should be promoted from passive to active.
   */
  PROMOTE_ENTITY_TO_ACTIVE,
  /**
   * Ask entity to sync a portion of its state.
   */
  SYNC_ENTITY,
  /**
   * An internally-created action to communicate that an entity should be loaded from its existing disk state.
   */
  LOAD_EXISTING_ENTITY;
}
