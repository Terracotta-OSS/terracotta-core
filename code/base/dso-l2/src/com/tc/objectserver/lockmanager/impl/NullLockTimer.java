/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.objectserver.lockmanager.api.LockAwardContext;
import com.tc.objectserver.lockmanager.api.LockEventListener;

public class NullLockTimer implements LockEventListener {

  public void notifyAddPending(int waiterCount, LockAwardContext ctxt) {
    return;
  }

  public void notifyAward(int waiterCount, LockAwardContext ctxt) {
    return;
  }

  public void notifyRevoke(LockAwardContext ctxt) {
    return;
  }

}
