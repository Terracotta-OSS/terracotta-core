/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.HashMap;
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

  private final Map<String, Integer>   unreadOperatorEventCount = new HashMap<String, Integer>();

  public Set<ServerGroupEntity> getServerGroupEntities() {
    return serverGroupEntities;
  }

  public Set<ClientEntity> getClientEntities() {
    return clientEntities;
  }

  public Map<String, Integer> getUnreadOperatorEventCount() {
    return unreadOperatorEventCount;
  }
}
