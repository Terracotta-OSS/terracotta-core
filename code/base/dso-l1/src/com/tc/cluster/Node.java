/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster;

public class Node {
  private final String nodeId;

  public Node(final String nodeId) {
    this.nodeId = nodeId;
  }

  public String getNodeId() {
    return nodeId;
  }
}
