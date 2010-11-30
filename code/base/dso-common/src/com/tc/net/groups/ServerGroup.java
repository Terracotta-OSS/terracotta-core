/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.HaConfigImpl;
import com.tc.config.ReloadConfigChangeContext;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.HaConfigSchema;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.net.GroupID;
import com.tc.object.config.schema.L2DSOConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGroup {

  private final GroupID     groupId;
  private String[]          members;
  private final HaConfigSchema haMode;
  private final Map         nodes;

  public ServerGroup(final ActiveServerGroupConfig group) {
    this.groupId = group.getGroupId();
    this.members = group.getMembers().getMemberArray();
    this.haMode = group.getHaHolder();
    this.nodes = new ConcurrentHashMap();
  }

  public ReloadConfigChangeContext reloadGroup(L2ConfigurationSetupManager manager,
                                               final ActiveServerGroupConfig group) throws ConfigurationSetupException {
    String[] membersBefore = this.members;
    String[] membersNow = group.getMembers().getMemberArray();
    this.members = group.getMembers().getMemberArray();

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
      nodesRemoved.add((Node) this.nodes.remove(member));
    }
  }

  private void addNodes(L2ConfigurationSetupManager configSetupManager, ActiveServerGroupConfig group,
                        List<Node> nodesAdded, String[] membersNowArray, String[] membersBeforeArray)
      throws ConfigurationSetupException {
    List<String> membersBefore = convertStringToList(membersBeforeArray);
    List<String> membersNow = convertStringToList(membersNowArray);
    membersNow.removeAll(membersBefore);
    for (String member : membersNow) {
      L2DSOConfig l2 = configSetupManager.dsoL2ConfigFor(member);
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
    Collection c = this.nodes.values();
    if (!ignoreCheck && c.size() != this.members.length) { throw new AssertionError(
                                                                                    "Not all members are present in this collection: collections=["
                                                                                        + getCollectionsToString(c)
                                                                                        + "] members=["
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
    for (String member : this.members) {
      out += member + " ";
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
