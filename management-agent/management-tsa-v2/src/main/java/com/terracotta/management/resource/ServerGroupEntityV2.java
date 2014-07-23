/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link org.terracotta.management.resource.AbstractEntityV2} representing a topology's server group
 * from the management API.
 *
 * @author Ludovic Orban
 */
public class ServerGroupEntityV2 extends AbstractTsaEntityV2 {

  private Integer id;
  private String name;
  private boolean coordinator;
  private Set<ServerEntityV2> servers = new HashSet<ServerEntityV2>();

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isCoordinator() {
    return coordinator;
  }

  public void setCoordinator(boolean coordinator) {
    this.coordinator = coordinator;
  }

  public Set<ServerEntityV2> getServers() {
    return servers;
  }
}
