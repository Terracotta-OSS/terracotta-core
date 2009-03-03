/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster.simulation;

import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoNode;

class SimulatedDsoClusterEvent implements DsoClusterEvent {

  private final DsoNode node;

  SimulatedDsoClusterEvent(final DsoNode node) {
    this.node = node;
  }

  public DsoNode getNode() {
    return node;
  }
}
