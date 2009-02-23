/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster;

import com.tc.net.NodeID;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DsoClusterTopologyImpl implements DsoClusterTopology {
  private final transient DsoClusterImpl     cluster;

  private final Map<NodeID, DsoNodeInternal> nodes = new HashMap<NodeID, DsoNodeInternal>();

  public DsoClusterTopologyImpl(final DsoClusterImpl cluster) {
    this.cluster = cluster;
  }

  public Collection<? extends DsoNode> getNodes() {
    return Collections.unmodifiableCollection(nodes.values());
  }

  DsoNodeInternal getDsoNode(final NodeID nodeId) {
    synchronized (this) {
      DsoNodeInternal node = nodes.get(nodeId);
      if (null == node) {
        node = registerDsoNode(nodeId);
      }

      Assert.assertNotNull(node);

      return node;
    }
  }

  DsoNodeInternal getAndRemoveDsoNode(final NodeID nodeId) {
    synchronized (this) {
      DsoNodeInternal node = nodes.remove(nodeId);
      // Assert.assertNotNull(node);
      return node;
    }
  }

  DsoNodeInternal registerDsoNode(final NodeID nodeId) {
    synchronized (this) {
      final DsoNodeInternal node = new DsoNodeImpl(cluster, nodeId);
      nodes.put(nodeId, node);
      return node;
    }
  }
}