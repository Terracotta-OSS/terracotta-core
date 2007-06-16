/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

public interface ZapNodeRequestProcessor {

  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason);

  public long[] getCurrentNodeWeights();

  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights);

}
