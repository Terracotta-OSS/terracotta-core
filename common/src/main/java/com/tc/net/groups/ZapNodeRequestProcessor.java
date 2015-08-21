/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public interface ZapNodeRequestProcessor {

  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason);

  public long[] getCurrentNodeWeights();

  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights);
  
  public void addZapEventListener(ZapEventListener listener);

}
