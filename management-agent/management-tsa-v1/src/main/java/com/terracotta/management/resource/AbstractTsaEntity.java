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

import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.VersionedEntity;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} abstract subclass from which topology entities
 * are derived from.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractTsaEntity extends VersionedEntity {

  private String agentId;

  public AbstractTsaEntity() {
    setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
  }

  @Override
  public String getAgentId() {
    return agentId;
  }

  @Override
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

}
