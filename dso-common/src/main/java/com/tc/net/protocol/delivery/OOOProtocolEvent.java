/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.util.Assert;

/**
 * 
 */
class OOOProtocolEvent {
  private final OOOProtocolMessage message;

  public OOOProtocolEvent(OOOProtocolMessage msg) {
    Assert.eval(msg != null);
    this.message = msg;
  }

  public OOOProtocolEvent() {
    this.message = null;
  }

  public void execute(AbstractStateMachine stateMachine) {
    stateMachine.execute(message);
  }
}
