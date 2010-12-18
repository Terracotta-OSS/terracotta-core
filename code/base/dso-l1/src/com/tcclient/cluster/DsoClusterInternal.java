/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.net.NodeID;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClusterMetaDataManager;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface DsoClusterInternal extends DsoCluster {

  public static enum EVENTS {
    THIS_NODE_JOIN("This Node Joined"), THIS_NODE_LEFT("This Node Left"), NODE_JOIN("Node Joined"), NODE_LEFT(
        "Node Left"), OPERATIONS_ENABLED("Operations Enabled"), OPERATIONS_DISABLED("Operations Disabled");

    private final String name;

    private EVENTS(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public void init(final ClusterMetaDataManager metaDataManager, final ClientObjectManager objectManager);

  public DsoNodeMetaData retrieveMetaDataForDsoNode(DsoNodeInternal node);

  public void fireThisNodeJoined(NodeID nodeId, NodeID[] clusterMembers);

  public void fireThisNodeLeft();

  public void fireNodeJoined(NodeID nodeId);

  public void fireNodeLeft(NodeID nodeId);

  public void fireOperationsEnabled();

  public void fireOperationsDisabled();

  public <K> Map<K, Set<DsoNode>> getNodesWithKeys(final Map<K, ?> map, final Collection<? extends K> keys)
      throws UnclusteredObjectException;

}