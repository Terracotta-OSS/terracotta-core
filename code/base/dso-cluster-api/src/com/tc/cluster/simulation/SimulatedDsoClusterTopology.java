/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster.simulation;

import com.tc.cluster.DsoClusterTopology;
import com.tcclient.cluster.DsoNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class SimulatedDsoClusterTopology implements DsoClusterTopology {

  private final Collection<DsoNode> nodes;

  SimulatedDsoClusterTopology(final DsoNode node) {
    final List<DsoNode> nodesList = new ArrayList<DsoNode>();
    nodesList.add(node);
    this.nodes = Collections.unmodifiableCollection(nodesList);
  }

  public Collection<DsoNode> getNodes() {
    return nodes;
  }
}
