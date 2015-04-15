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
package com.terracotta.management.resource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing a TSA topology from the management API.
 *
 * @author Ludovic Orban
 */
public class TopologyEntity extends AbstractTsaEntity {

  private final Set<ServerGroupEntity> serverGroupEntities = new HashSet<ServerGroupEntity>();
  private final Set<ClientEntity> clientEntities = new HashSet<ClientEntity>();

  private Map<String, Integer>   unreadOperatorEventCount;

  public Set<ServerGroupEntity> getServerGroupEntities() {
    return serverGroupEntities;
  }

  public Set<ClientEntity> getClientEntities() {
    return clientEntities;
  }

  public void setUnreadOperatorEventCount(Map<String, Integer> unreadCount) {
    this.unreadOperatorEventCount = unreadCount;
  }

  public Map<String, Integer> getUnreadOperatorEventCount() {
    return unreadOperatorEventCount;
  }
}
