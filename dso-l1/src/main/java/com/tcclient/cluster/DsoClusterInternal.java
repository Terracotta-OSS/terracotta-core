/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.cluster;

import com.tc.async.api.Stage;
import com.tc.cluster.DsoCluster;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.object.ClientObjectManager;
import com.tc.object.ClusterMetaDataManager;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface DsoClusterInternal extends DsoCluster, DsoClusterInternalEventsGun, DsoClusterEventsNotifier {

  public static enum DsoClusterEventType {
    NODE_JOIN("Node Joined"), NODE_LEFT("Node Left"), OPERATIONS_ENABLED("Operations Enabled"), OPERATIONS_DISABLED(
        "Operations Disabled");

    private final String name;

    private DsoClusterEventType(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public void init(final ClusterMetaDataManager metaDataManager, final ClientObjectManager objectManager,
                   Stage dsoClusterEventsStage);

  public DsoNodeMetaData retrieveMetaDataForDsoNode(DsoNodeInternal node);

  public <K> Map<K, Set<DsoNode>> getNodesWithKeys(final Map<K, ?> map, final Collection<? extends K> keys)
      throws UnclusteredObjectException;

}