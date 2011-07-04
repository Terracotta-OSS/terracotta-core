/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.context;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;

public class PauseContext implements EventContext {

  private final boolean isPause;
  private final NodeID  remoteNode;

  public PauseContext(final boolean isPause, final NodeID remoteNode) {
    this.isPause = isPause;
    this.remoteNode = remoteNode;
  }

  public boolean getIsPause() {
    return this.isPause;
  }

  public NodeID getRemoteNode() {
    return this.remoteNode;
  }

}
