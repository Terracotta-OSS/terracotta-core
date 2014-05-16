/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import org.terracotta.management.resource.AgentEntityV2;
import org.terracotta.management.resource.VersionedEntityV2;

/**
 * A {@link org.terracotta.management.resource.VersionedEntityV2} abstract subclass from which topology entities
 * are derived from.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractTsaEntityV2 extends VersionedEntityV2 {

  private String agentId;

  public AbstractTsaEntityV2() {
    setAgentId(AgentEntityV2.EMBEDDED_AGENT_ID);
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
