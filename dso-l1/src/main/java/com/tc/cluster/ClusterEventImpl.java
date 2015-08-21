/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

import com.tcclient.cluster.Node;

public class ClusterEventImpl implements ClusterEvent {

  private final Node node;

  public ClusterEventImpl(Node node) {
    this.node = node;
  }

  @Override
  public Node getNode() {
    return node;
  }

  @Override
  public String toString() {
    return "DsoClusterEvent:" + getNode().toString();
  }
}
