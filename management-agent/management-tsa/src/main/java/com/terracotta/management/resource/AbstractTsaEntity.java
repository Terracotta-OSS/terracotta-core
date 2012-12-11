/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
