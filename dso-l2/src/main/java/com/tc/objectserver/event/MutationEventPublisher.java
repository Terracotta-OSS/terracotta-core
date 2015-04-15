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
package com.tc.objectserver.event;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.managedobject.CDSMValue;
import com.tc.server.ServerEventType;

import java.util.Set;

/**
 * Records all cache modifications and maps keys to actual values
 * omiting intermediate object id.
 *
 * @author Eugene Shelestovich
 */
public interface MutationEventPublisher {

  /**
   * This method records an <code>AdvancedServerEvent</code>.
   * 
   * @param clientIds
   */
  void publishEvent(Set<ClientID> clientIds, ServerEventType type, Object key, CDSMValue value, String cacheName);

  void setBytesForObjectID(ObjectID objectId, byte[] value);
}
