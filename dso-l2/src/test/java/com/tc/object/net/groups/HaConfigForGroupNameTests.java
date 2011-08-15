/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net.groups;

import com.tc.config.ClusterInfo;
import com.tc.config.HaConfig;
import com.tc.config.NodesStore;
import com.tc.config.ReloadConfigChangeContext;
import com.tc.exception.ImplementMe;
import com.tc.net.GroupID;
import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;

import java.util.HashSet;
import java.util.Set;

public class HaConfigForGroupNameTests implements HaConfig {

  private final ClusterInfo set;

  public HaConfigForGroupNameTests(Set<String> tempSet) {
    this.set = new ClusterInfoImpl(tempSet);
  }

  public GroupID getActiveCoordinatorGroupID() {
    throw new ImplementMe();
  }

  public ServerGroup[] getAllActiveServerGroups() {
    throw new ImplementMe();
  }

  public Node[] getAllNodes() {
    throw new ImplementMe();
  }

  public GroupID getThisGroupID() {
    throw new ImplementMe();
  }

  public GroupID[] getGroupIDs() {
    throw new ImplementMe();
  }

  public ClusterInfo getClusterInfo() {
    return this.set;
  }

  public ServerGroup getThisGroup() {
    throw new ImplementMe();
  }

  public Node[] getThisGroupNodes() {
    throw new ImplementMe();
  }

  public Node getThisNode() {
    throw new ImplementMe();
  }

  public boolean isActiveActive() {
    throw new ImplementMe();
  }

  public boolean isActiveCoordinatorGroup() {
    throw new ImplementMe();
  }

  public boolean isDiskedBasedActivePassive() {
    throw new ImplementMe();
  }

  public boolean isNetworkedActivePassive() {
    throw new ImplementMe();
  }

  public ReloadConfigChangeContext reloadConfiguration() {
    throw new ImplementMe();
  }

  public NodesStore getNodesStore() {
    throw new ImplementMe();
  }

  public static class ClusterInfoImpl implements ClusterInfo {
    public volatile HashSet<String> serverNamesForThisGroup = new HashSet<String>();

    public ClusterInfoImpl(Set<String> set) {
      this.serverNamesForThisGroup.addAll(set);
    }

    public boolean hasServerInGroup(String serverName) {
      return serverNamesForThisGroup.contains(serverName);
    }

    void updateServerNames(ReloadConfigChangeContext context) {
      HashSet<String> tmp = (HashSet<String>) serverNamesForThisGroup.clone();

      for (Node n : context.getNodesAdded()) {
        tmp.add(n.getServerNodeName());
      }

      for (Node n : context.getNodesRemoved()) {
        tmp.remove(n.getServerNodeName());
      }

      this.serverNamesForThisGroup = tmp;
    }

    public GroupID getGroupIDFromServerName(String name) {
      throw new ImplementMe();
    }

    public boolean hasServerInCluster(String name) {
      throw new ImplementMe();
    }

  }

  public String getNodeName(String member) {
    return null;
  }
}
