/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net.groups;

import com.tc.config.ClusterInfo;
import com.tc.config.HaConfig;
import com.tc.config.NodesStore;
import com.tc.config.ReloadConfigChangeContext;
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

  @Override
  public GroupID getActiveCoordinatorGroupID() {
    throw new UnsupportedOperationException();
  }

  public ServerGroup[] getAllActiveServerGroups() {
    throw new UnsupportedOperationException();
  }

  public Node[] getAllNodes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public GroupID getThisGroupID() {
    throw new UnsupportedOperationException();
  }

  @Override
  public GroupID[] getGroupIDs() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return this.set;
  }

  public ServerGroup getThisGroup() {
    throw new UnsupportedOperationException();
  }

  public Node[] getThisGroupNodes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Node getThisNode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActiveActive() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActiveCoordinatorGroup() {
    throw new UnsupportedOperationException();
  }

  public boolean isDiskedBasedActivePassive() {
    throw new UnsupportedOperationException();
  }

  public boolean isNetworkedActivePassive() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReloadConfigChangeContext reloadConfiguration() {
    throw new UnsupportedOperationException();
  }

  @Override
  public NodesStore getNodesStore() {
    throw new UnsupportedOperationException();
  }

  public static class ClusterInfoImpl implements ClusterInfo {
    public volatile Set<String> serverNamesForThisGroup = new HashSet<>();

    public ClusterInfoImpl(Set<String> set) {
      this.serverNamesForThisGroup.addAll(set);
    }

    @Override
    public boolean hasServerInGroup(String serverName) {
      return serverNamesForThisGroup.contains(serverName);
    }

    void updateServerNames(ReloadConfigChangeContext context) {
      Set<String> tmp = new HashSet<>(serverNamesForThisGroup);

      for (Node n : context.getNodesAdded()) {
        tmp.add(n.getServerNodeName());
      }

      for (Node n : context.getNodesRemoved()) {
        tmp.remove(n.getServerNodeName());
      }

      this.serverNamesForThisGroup = tmp;
    }

    @Override
    public GroupID getGroupIDFromNodeName(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasServerInCluster(String name) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public String getNodeName(String member) {
    return null;
  }
}
