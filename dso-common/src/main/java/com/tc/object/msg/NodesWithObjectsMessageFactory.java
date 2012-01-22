/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;

public interface NodesWithObjectsMessageFactory {

  public NodesWithObjectsMessage newNodesWithObjectsMessage(NodeID nodeID);

}
