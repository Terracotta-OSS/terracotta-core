/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.GroupID;
import com.tc.net.groups.Node;

public interface HaConfig {

  /**
   * Returns true if more than 1 ActiveServerGroup's are defined
   */
  boolean isActiveActive();

  GroupID getActiveCoordinatorGroupID();

  GroupID[] getGroupIDs();

  Node getThisNode();

  GroupID getThisGroupID();

  NodesStore getNodesStore();

  boolean isActiveCoordinatorGroup();

  /**
   * @return true if nodes are removed
   * @throws ConfigurationSetupException
   */
  ReloadConfigChangeContext reloadConfiguration() throws ConfigurationSetupException;

  ClusterInfo getClusterInfo();

  String getNodeName(String member);
}
