/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.webflow.conversation.impl.ConversationLock;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

/**
 * Clustered conversation lock 
 * 
 * @author Eugene Kuleshov
 */
class DSOConversationLock implements ConversationLock {

  public void lock() {
    ManagerUtil.monitorEnter(this, Manager.LOCK_TYPE_WRITE);
  }

  public void unlock() {
    ManagerUtil.monitorExit(this);
  }
}
