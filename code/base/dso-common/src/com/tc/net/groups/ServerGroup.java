/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.NewHaConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ServerGroup {

  private final int         groupId;
  private final String[]    members;
  private final NewHaConfig haMode;
  private final Map         nodes;

  public ServerGroup(final ActiveServerGroupConfig group) {
    this.groupId = group.getId();
    this.members = group.getMembers().getMemberArray();
    this.haMode = group.getHa();
    this.nodes = new HashMap();
  }

  public int getGroupId() {
    return groupId;
  }

  public Collection getNodes() {
    Collection c = this.nodes.values();
    if (c.size() != this.members.length) { throw new AssertionError(
                                                                    "Not all members are present in this collection: collections=["
                                                                        + getCollectionsToString(c) + "] members=["
                                                                        + getMembersToString() + "]"); }
    return c;
  }

  private String getCollectionsToString(Collection c) {
    String out = "";
    for (Iterator iter = c.iterator(); iter.hasNext();) {
      Node node = (Node) iter.next();
      out += node.toString() + " ";
    }
    return out;
  }

  private String getMembersToString() {
    String out = "";
    for (int i = 0; i < this.members.length; i++) {
      out += members[i] + " ";
    }
    return out;
  }

  public void addNode(Node node, String serverName) {
    if (!hasMember(serverName)) { throw new AssertionError("Server=[" + serverName
                                                           + "] is not a member of activeServerGroup=[" + this.groupId
                                                           + "]"); }
    this.nodes.put(serverName, node);
  }

  public Node getNode(String serverName) {
    return (Node) this.nodes.get(serverName);
  }

  public boolean isNetworkedActivePassive() {
    return this.haMode.isNetworkedActivePassive();
  }

  public int getElectionTime() {
    return this.haMode.electionTime();
  }

  public boolean equals(Object obj) {
    if (obj instanceof ServerGroup) {
      ServerGroup that = (ServerGroup) obj;
      return this.groupId == that.groupId;
    }
    return false;
  }

  public int hashCode() {
    return groupId;
  }

  public String toString() {
    return "ActiveServerGroup{groupId=" + groupId + "}";
  }

  public boolean hasMember(String serverName) {
    for (int i = 0; i < this.members.length; i++) {
      if (members[i].equals(serverName)) { return true; }
    }
    return false;
  }
}
