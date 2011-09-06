/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;

public class DefaultZapNodeRequestProcessor implements ZapNodeRequestProcessor {

  private final TCLogger logger;

  public DefaultZapNodeRequestProcessor(final TCLogger logger) {
    this.logger = logger;
  }

  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason) {
    return true;
  }

  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    this.logger.fatal("DefaultZapNodeRequestProcessor : Received Zap Node request from " + nodeID + " type = "
                      + zapNodeType + " reason = " + reason);
    System.exit(zapNodeType);
  }

  public long[] getCurrentNodeWeights() {
    return new long[0];
  }

  public void addZapEventListener(ZapEventListener listener) {
    //
  }

}
