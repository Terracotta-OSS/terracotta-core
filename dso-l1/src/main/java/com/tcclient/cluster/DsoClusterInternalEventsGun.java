/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.net.NodeID;

public interface DsoClusterInternalEventsGun {

  public void fireThisNodeJoined(NodeID nodeId, NodeID[] clusterMembers);

  public void fireThisNodeLeft();

  public void fireNodeJoined(NodeID nodeId);

  public void fireNodeLeft(NodeID nodeId);

  public void fireOperationsEnabled();

  public void fireOperationsDisabled();

}
