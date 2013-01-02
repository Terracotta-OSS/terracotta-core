/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.logging.TCLogger;
import com.tc.net.NodeID;

public class DefaultZapNodeRequestProcessor implements ZapNodeRequestProcessor {

  private final TCLogger logger;

  public DefaultZapNodeRequestProcessor(final TCLogger logger) {
    this.logger = logger;
  }

  @Override
  public boolean acceptOutgoingZapNodeRequest(NodeID nodeID, int type, String reason) {
    return true;
  }

  @Override
  public void incomingZapNodeRequest(NodeID nodeID, int zapNodeType, String reason, long[] weights) {
    this.logger.fatal("DefaultZapNodeRequestProcessor : Received Zap Node request from " + nodeID + " type = "
                      + zapNodeType + " reason = " + reason);
    System.exit(zapNodeType);
  }

  @Override
  public long[] getCurrentNodeWeights() {
    return new long[0];
  }

  @Override
  public void addZapEventListener(ZapEventListener listener) {
    //
  }

}
