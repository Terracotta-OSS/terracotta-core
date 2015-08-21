/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.ServerID;

import java.util.Set;

public interface ActiveServerIDManager {

  public Set<ServerID> getAllActiveServerIDs();

  public ServerID getActiveServerIDFor(GroupID groupID);

  public boolean isBlackListedServer(NodeID serverID);

  public void addActiveServerListener(ActiveServerListener listener);

  public void removeActiveServerListener(ActiveServerListener listener);

  public GroupID getLocalGroupID();

}