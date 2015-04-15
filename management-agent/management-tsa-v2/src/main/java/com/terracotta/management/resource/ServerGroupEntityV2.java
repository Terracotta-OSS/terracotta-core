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
