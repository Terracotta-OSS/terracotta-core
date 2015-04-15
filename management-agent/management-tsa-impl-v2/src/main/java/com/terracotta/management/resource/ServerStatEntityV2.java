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

import java.io.Serializable;

public class ServerStatEntityV2 implements Serializable {

  private  String health;
  private  String role;
  private  String state;
  private  String managementPort;
  private  String serverGroupName;
  private  String name;

  public ServerStatEntityV2() {
  }
  
  public ServerStatEntityV2(String health, String role, String state, String managementPort, String serverGroupName, String serverName) {
    this.health = health;
    this.role = role;
    this.state = state;
    this.managementPort = managementPort;
    this.serverGroupName = serverGroupName;
    this.name = serverName;
  }
  
  public String getHealth() {
    return health;
  }

  public void setHealth(String health) {
    this.health = health;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getManagementPort() {
    return managementPort;
  }

  public void setManagementPort(String managementPort) {
    this.managementPort = managementPort;
  }

  public String getServerGroupName() {
    return serverGroupName;
  }

  public void setServerGroupName(String serverGroupName) {
    this.serverGroupName = serverGroupName;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }
}
