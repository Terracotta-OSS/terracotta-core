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
package com.tc.objectserver.entity;

import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.net.DSOChannelManagerEventListener;


/**
 * This interface tracks the connections between clients and entities, created by a fetch and destroyed by a release.
 * Note that the same client can refer to a specific entity multiple times, which each connection possessing a distinct
 * life cycle.
 */
public interface ClientEntityStateManager extends DSOChannelManagerEventListener {
  /**
   * Adds a reference from clientID to the entity described by entityDescriptor.
   * 
   * @param clientID The client.
   * @param entityDescriptor The entity.
   */
  public boolean addReference(ClientID clientID, EntityDescriptor entityDescriptor);

  /**
   * Removes a reference from clientID to the entity described by entityDescriptor.
   * NOTE:  This will assert if an attempt is made to remove a reference which was never added.
   * 
   * @param clientID The client.
   * @param entityDescriptor The entity.
   */
  public boolean removeReference(ClientID clientID, EntityDescriptor entityDescriptor);

  /**
   * Verifies that no clients have a reference to the entity described by entityDescriptor.
   * NOTE:  This will assert if there are any remaining references.
   * 
   * @param entityDescriptor The entity.
   * @return true if there are no references
   */
  public boolean verifyNoReferences(EntityID entityDescriptor);
}
