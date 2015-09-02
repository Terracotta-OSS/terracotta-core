/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.HaConfigImpl;
import com.tc.config.ReloadConfigChangeContext;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.net.GroupID;
import com.tc.object.config.schema.L2Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGroup {

  private final GroupID           groupId;
  private volatile String[]       members;
  private final Map<String, Node> nodes;

  public ServerGroup(ActiveServerGroupConfig group) {
    this.groupId = group.getGroupId();
    this.members = group.getMembers();
    this.nodes = new ConcurrentHashMap<String, Node>();
  }

  public ReloadConfigChangeContext reloadGroup(L2ConfigurationSetupManager manager, ActiveServerGroupConfig group)
      throws ConfigurationSetupException {
    String[] membersBefore = this.members;
    String[] membersNow = group.getMembers();
    this.members = group.getMembers();

    ReloadConfigChangeContext context = new ReloadConfigChangeContext();
    addNodes(manager, group, context.getNodesAdded(), membersNow, membersBefore);
    removeNodes(context.getNodesRemoved(), membersNow, membersBefore);
    return context;
  }

  private ArrayList<String> convertStringToList(String[] strArray) {
    ArrayList<String> list = new ArrayList<String>();
    for (String str : strArray) {
      list.add(str);
    }
    return list;
  }

  private void removeNodes(List<Node> nodesRemoved, String[] membersNowArray, String[] membersBeforeArray) {
    List<String> membersBefore = convertStringToList(membersBeforeArray);
    List<String> membersNow = convertStringToList(membersNowArray);
    membersBefore.removeAll(membersNow);
    for (String member : membersBefore) {
      nodesRemoved.add(this.nodes.remove(member));
    }
  }

  private void addNodes(L2ConfigurationSetupManager configSetupManager, ActiveServerGroupConfig group,
                        List<Node> nodesAdded, String[] membersNowArray, String[] membersBeforeArray)
      throws ConfigurationSetupException {
    List<String> membersBefore = convertStringToList(membersBeforeArray);
    List<String> membersNow = convertStringToList(membersNowArray);
    membersNow.removeAll(membersBefore);
    for (String member : membersNow) {
      L2Config l2 = configSetupManager.dsoL2ConfigFor(member);
      Node node = HaConfigImpl.makeNode(l2);
      nodesAdded.add(node);
      this.addNode(node, member);
    }
  }

  public GroupID getGroupId() {
    return groupId;
  }

  public Collection<Node> getNodes() {
    return getNodes(false);
  }

  public Collection<Node> getNodes(boolean ignoreCheck) {
    Collection<Node> c = this.nodes.values();
    if (!ignoreCheck && c.size() != this.members.length) { throw new AssertionError(
                                                                                    "Not all members are present in this collection: collections=["
                                                                                        + getCollectionsToString(c)
                                                                                        + "] members=["
                                                                                        + getMembersToString() + "]"); }
    return c;
  }

  private String getCollectionsToString(Collection<Node> c) {
    StringBuilder sb = new StringBuilder();
    for (Node node : c) {
      sb.append(node.toString() + " ");
    }
    return sb.toString();
  }

  private String getMembersToString() {
    StringBuilder sb = new StringBuilder();
    for (String member : this.members) {
      sb.append(member + " ");
    }
    return sb.toString();
  }

  public void addNode(Node node, String serverName) {
    if (!hasMember(serverName)) { throw new AssertionError("Server=[" + serverName
                                                           + "] is not a member of activeServerGroup=[" + this.groupId
                                                           + "]"); }
    this.nodes.put(serverName, node);
  }

  public Node getNode(String serverName) {
    return this.nodes.get(serverName);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServerGroup) {
      ServerGroup that = (ServerGroup) obj;
      return this.groupId == that.groupId;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return groupId.toInt();
  }

  @Override
  public String toString() {
    return "ActiveServerGroup{groupId=" + groupId + "}";
  }

  public boolean hasMember(String serverName) {
    for (String member : this.members) {
      if (member.equals(serverName)) { return true; }
    }
    return false;
  }

  public List<String> getMembers() {
    return Arrays.asList(members);
  }
}
