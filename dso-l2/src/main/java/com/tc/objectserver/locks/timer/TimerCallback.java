/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks.timer;

import com.tc.objectserver.locks.timer.LockTimer.LockTimerContext;


public interface TimerCallback {
  public void timerTimeout(LockTimerContext callbackObject);
}
