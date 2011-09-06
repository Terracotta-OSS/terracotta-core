/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.net.groups.TCGroupManagerImpl;
import com.tc.net.groups.TCGroupMessageWrapper;

public class ReceiveGroupMessageHandler extends AbstractEventHandler {
  private final TCGroupManagerImpl manager;
  
  public ReceiveGroupMessageHandler(TCGroupManagerImpl manager) {
    this.manager = manager;
  }
  
  public void handleEvent(EventContext context) {
    TCGroupMessageWrapper wrapper = (TCGroupMessageWrapper) context;
    manager.messageReceived(wrapper.getGroupMessage(), wrapper.getChannel());
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
  }

}