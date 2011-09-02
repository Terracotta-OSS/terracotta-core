/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  /**
   * Returns true if not active-active and ha mode is set to networked-active-passive
   */
  boolean isNetworkedActivePassive();

  /**
   * Returns true if not active-active and not networked-active-passive
   */
  boolean isDiskedBasedActivePassive();

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
