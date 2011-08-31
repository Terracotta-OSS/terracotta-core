/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tcclient.cluster.DsoNode;

public class DsoClusterEventImpl implements DsoClusterEvent {

  private final DsoNode node;

  public DsoClusterEventImpl(final DsoNode node) {
    this.node = node;
  }

  public DsoNode getNode() {
    return node;
  }

  @Override
  public String toString() {
    return "DsoClusterEvent:" + getNode().toString();
  }
}
