/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing a TSA topology from the management API.
 *
 * @author Ludovic Orban
 */
public class TopologyEntity extends AbstractTsaEntity {

  private Set<ServerGroupEntity> serverGroupEntities = new HashSet<ServerGroupEntity>();

  public Set<ServerGroupEntity> getServerGroupEntities() {
    return serverGroupEntities;
  }

}
