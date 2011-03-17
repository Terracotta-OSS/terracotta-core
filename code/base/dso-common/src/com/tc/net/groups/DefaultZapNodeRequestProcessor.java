/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public class DefaultZapNodeRequestProcessor implements ZapNodeRequestProcessor {

  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason) {
    return true;
  }

  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    throw new AssertionError("DefaultZapNodeRequestProcessor : Received Zap Node request from " + nodeID + " type = "
                             + zapNodeType + " reason = " + reason);
  }

  public long[] getCurrentNodeWeights() {
    return new long[0];
  }

  public void addZapEventListener(ZapEventListener listener) {
    //
  }

}
