/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource;

import java.io.Serializable;

public class ServerStatEntityV2 implements Serializable {

  private  String health;
  private  String role;
  private  String state;
  private  String managementPort;
  private  String serverGroupName;

  public ServerStatEntityV2() {
  }
  
  public ServerStatEntityV2(String health, String role, String state, String managementPort, String serverGroupName) {
    this.health = health;
    this.role = role;
    this.state = state;
    this.managementPort = managementPort;
    this.serverGroupName = serverGroupName;
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


}
