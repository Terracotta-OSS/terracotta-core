/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.ServerID;

public interface ActiveServerListener {

  public void activeServerJoined(GroupID groupID, ServerID serverID);

  public void activeServerLeft(GroupID groupID, ServerID serverID);
}
