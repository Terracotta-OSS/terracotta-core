/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public interface ZapEventListener {

  void fireSplitBrainEvent(NodeID node1, NodeID node2);

  void fireBackOffEvent(NodeID winnerNode);
}
