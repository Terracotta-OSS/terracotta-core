/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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