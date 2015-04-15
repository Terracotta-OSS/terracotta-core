/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  @Override
  public GroupID getActiveCoordinatorGroupID() {
    throw new ImplementMe();
  }

  public ServerGroup[] getAllActiveServerGroups() {
    throw new ImplementMe();
  }

  public Node[] getAllNodes() {
    throw new ImplementMe();
  }

  @Override
  public GroupID getThisGroupID() {
    throw new ImplementMe();
  }

  @Override
  public GroupID[] getGroupIDs() {
    throw new ImplementMe();
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return this.set;
  }

  public ServerGroup getThisGroup() {
    throw new ImplementMe();
  }

  public Node[] getThisGroupNodes() {
    throw new ImplementMe();
  }

  @Override
  public Node getThisNode() {
    throw new ImplementMe();
  }

  @Override
  public boolean isActiveActive() {
    throw new ImplementMe();
  }

  @Override
  public boolean isActiveCoordinatorGroup() {
    throw new ImplementMe();
  }

  public boolean isDiskedBasedActivePassive() {
    throw new ImplementMe();
  }

  public boolean isNetworkedActivePassive() {
    throw new ImplementMe();
  }

  @Override
  public ReloadConfigChangeContext reloadConfiguration() {
    throw new ImplementMe();
  }

  @Override
  public NodesStore getNodesStore() {
    throw new ImplementMe();
  }

  public static class ClusterInfoImpl implements ClusterInfo {
    public volatile HashSet<String> serverNamesForThisGroup = new HashSet<String>();

    public ClusterInfoImpl(Set<String> set) {
      this.serverNamesForThisGroup.addAll(set);
    }

    @Override
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

    @Override
    public GroupID getGroupIDFromNodeName(String name) {
      throw new ImplementMe();
    }

    @Override
    public boolean hasServerInCluster(String name) {
      throw new ImplementMe();
    }
  }

  @Override
  public String getNodeName(String member) {
    return null;
  }
}
