/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

// TODO: evaluate what to do with this now that there's ClusterEventsNG
public class Node {
  private final String nodeId;

  public Node(final String nodeId) {
    this.nodeId = nodeId;
  }

  public String getNodeId() {
    return nodeId;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Node)) return false;
    Node that = (Node) obj;
    return this.nodeId.equals(that.nodeId);
  }

  @Override
  public int hashCode() {
    return nodeId.hashCode();
  }

  @Override
  public String toString() {
    return nodeId;
  }
}
