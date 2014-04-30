/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.ServerID;

public interface PassiveServerListener {
  public void passiveServerJoined(ServerID serverID);

  public void passiveServerLeft(ServerID serverID);
}
