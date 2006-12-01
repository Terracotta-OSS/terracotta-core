/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring.beans.orm.domain;

public class Permission {

  private int permissionId;
  private String name;

  public String toString() {
    StringBuffer result = new StringBuffer(50);
    result.append("Permission { permissionId=");
    result.append(permissionId);
    result.append(", name=");
    result.append(name);
    result.append(" }");
    return result.toString();
  } 
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getPermissionId() {
    return permissionId;
  }

  public void setPermissionId(int permissionId) {
    this.permissionId = permissionId;
  }
}
