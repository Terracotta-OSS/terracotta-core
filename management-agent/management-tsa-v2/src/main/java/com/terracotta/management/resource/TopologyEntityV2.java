/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing a TSA topology from the management API.
 *
 * @author Ludovic Orban
 */
public class TopologyEntityV2 extends AbstractTsaEntityV2 {

  private final Set<ServerGroupEntityV2> serverGroupEntities = new HashSet<ServerGroupEntityV2>();
  private final Set<ClientEntityV2>      clientEntities = new HashSet<ClientEntityV2>();
  private final Map<String, Integer>     unreadOperatorEventCount = new HashMap<String, Integer>();

  public Set<ServerGroupEntityV2> getServerGroupEntities() {
    return serverGroupEntities;
  }

  public Set<ClientEntityV2> getClientEntities() {
    return clientEntities;
  }

  public Map<String, Integer> getUnreadOperatorEventCount() {
    return unreadOperatorEventCount;
  }

}
