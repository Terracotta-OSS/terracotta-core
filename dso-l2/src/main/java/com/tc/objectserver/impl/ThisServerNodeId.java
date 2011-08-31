/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.net.ServerID;

public class ThisServerNodeId {

  private static volatile ServerID thisServerNodeId = new ServerID();

  public static ServerID getThisServerNodeId() {
    return thisServerNodeId;
  }

  public static void setThisServerNodeId(ServerID id) {
    thisServerNodeId = id;
  }

}
