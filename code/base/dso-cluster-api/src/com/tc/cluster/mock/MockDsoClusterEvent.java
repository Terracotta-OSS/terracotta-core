/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.cluster.mock;

import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoNode;

class MockDsoClusterEvent implements DsoClusterEvent {

  private final DsoNode node;

  MockDsoClusterEvent(final DsoNode node) {
    this.node = node;
  }

  public DsoNode getNode() {
    return node;
  }
}
