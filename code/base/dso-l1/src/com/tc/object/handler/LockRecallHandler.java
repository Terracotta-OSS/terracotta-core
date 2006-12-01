/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.msg.LockResponseMessage;

public class LockRecallHandler extends AbstractEventHandler {

  private ClientLockManager lockManager;
  
  public void handleEvent(EventContext context) {
    LockResponseMessage msg = (LockResponseMessage) context;
    lockManager.recall(msg.getLockID(), msg.getThreadID(), msg.getLockLevel());
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }

}
