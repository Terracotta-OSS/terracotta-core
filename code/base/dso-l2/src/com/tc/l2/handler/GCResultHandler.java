/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.msg.GCResultMessage;
import com.tc.l2.objectserver.ReplicatedObjectManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;

public class GCResultHandler extends AbstractEventHandler {

  private ReplicatedObjectManager rObjectManager;

  public void handleEvent(EventContext context) {
    rObjectManager.handleGCResult((GCResultMessage)context);
  }
  
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.rObjectManager = oscc.getL2Coordinator().getReplicatedObjectManager();
  }
}
