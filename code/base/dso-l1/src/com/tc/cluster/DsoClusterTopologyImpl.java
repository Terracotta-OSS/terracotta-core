/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

public class DsoClusterTopologyImpl implements DsoClusterTopology {
  private final Map<String, DsoNode> nodes = new HashMap<String, DsoNode>();

  public Collection<DsoNode> getNodes() {
    return Collections.unmodifiableCollection(nodes.values());
  }

  DsoNode getDsoNode(final String nodeId) {
    synchronized (this) {
      DsoNode node = nodes.get(nodeId);
      if (null == node) {
        node = registerDsoNode(nodeId);
      }

      Assert.assertNotNull(node);

      return node;
    }
  }

  DsoNode getAndRemoveDsoNode(final String nodeId) {
    synchronized (this) {
      DsoNode node = nodes.remove(nodeId);
      Assert.assertNotNull(node);
      return node;
    }
  }

  DsoNode registerDsoNode(final String nodeId) {
    synchronized (this) {
      final DsoNode node = new DsoNodeImpl(nodeId);
      nodes.put(nodeId, node);
      return node;
    }
  }
}