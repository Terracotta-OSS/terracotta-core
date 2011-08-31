/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import java.io.Serializable;

public class ServerGroupInfo implements Serializable {
  private final L2Info[] members;
  private final String   name;
  private final int      id;
  private final boolean  isCoordinator;

  public ServerGroupInfo(L2Info[] members, String name, int id, boolean isCoordinator) {
    this.members = members;
    this.name = name;
    this.id = id;
    this.isCoordinator = isCoordinator;
  }

  public L2Info[] members() {
    return members;
  }

  public String name() {
    return name;
  }

  public int id() {
    return id;
  }

  public boolean isCoordinator() {
    return isCoordinator;
  }
}
