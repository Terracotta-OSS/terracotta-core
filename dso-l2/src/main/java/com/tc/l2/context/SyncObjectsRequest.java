/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;

public class SyncObjectsRequest implements EventContext {

  private final NodeID nodeID;

  public SyncObjectsRequest(NodeID nodeID) {
    this.nodeID = nodeID;
  }
  
  public NodeID getNodeID() {
    return nodeID;
  }

}
