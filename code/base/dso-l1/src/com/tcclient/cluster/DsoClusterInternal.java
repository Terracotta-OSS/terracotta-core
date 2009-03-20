/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.cluster.DsoCluster;
import com.tc.net.NodeID;

public interface DsoClusterInternal extends DsoCluster {

  public DsoNodeMetaData retrieveMetaDataForDsoNode(DsoNodeInternal node);

  public void fireThisNodeJoined(NodeID nodeId, NodeID[] clusterMembers);

  public void fireThisNodeLeft();

  public void fireNodeJoined(NodeID nodeId);

  public void fireNodeLeft(NodeID nodeId);

  public void fireOperationsEnabled();

  public void fireOperationsDisabled();

}