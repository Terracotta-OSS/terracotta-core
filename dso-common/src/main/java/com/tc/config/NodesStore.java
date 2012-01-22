/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.groups.Node;

public interface NodesStore extends ClusterInfo {

  void registerForTopologyChange(TopologyChangeListener listener);

  Node[] getAllNodes();
  
  String getNodeNameFromServerName(String serverName);
}
