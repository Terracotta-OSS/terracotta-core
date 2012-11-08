/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.net.ClientID;
import com.tc.net.NodeID;

public interface DsoClusterInternalEventsGun {

  void fireThisNodeJoined(NodeID nodeId, NodeID[] clusterMembers);

  void fireThisNodeLeft();

  void fireNodeJoined(NodeID nodeId);

  void fireNodeLeft(NodeID nodeId);

  void fireOperationsEnabled();

  void fireOperationsDisabled();

  void fireNodeRejoined(ClientID oldNodeId, ClientID newNodeId);

}
