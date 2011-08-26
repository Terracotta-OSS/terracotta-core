/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.RemoteObjectManager;
import com.tc.object.msg.RequestRootResponseMessage;

public class ReceiveRootIDHandler extends AbstractEventHandler {
  private RemoteObjectManager objectManager;

  public void handleEvent(EventContext context) {
    RequestRootResponseMessage m = (RequestRootResponseMessage) context;
    this.objectManager.addRoot(m.getRootName(), m.getRootID(), m.getSourceNodeID());

  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.objectManager = ccc.getObjectManager();
  }

}