/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster.mock;

import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.DsoNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class MockDsoClusterTopology implements DsoClusterTopology {

  private final Collection<DsoNode> mockNodes;

  MockDsoClusterTopology(final DsoNode mockNode) {
    final List<DsoNode> nodes = new ArrayList<DsoNode>();
    nodes.add(mockNode);
    this.mockNodes = Collections.unmodifiableCollection(nodes);
  }

  public Collection<DsoNode> getNodes() {
    return mockNodes;
  }
}
