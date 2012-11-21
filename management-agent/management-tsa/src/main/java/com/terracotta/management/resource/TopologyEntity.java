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
  private Set<ClientEntity> clientEntities = new HashSet<ClientEntity>();

  public Set<ServerGroupEntity> getServerGroupEntities() {
    return serverGroupEntities;
  }

  public Set<ClientEntity> getClientEntities() {
    return clientEntities;
  }

}
