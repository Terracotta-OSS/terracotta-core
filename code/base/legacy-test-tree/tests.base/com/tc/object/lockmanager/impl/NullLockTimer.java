/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.TimerCallback;
import com.tc.object.tx.TimerSpec;

import java.util.Timer;
import java.util.TimerTask;

public class NullLockTimer implements TCLockTimer {

  public TimerTask scheduleTimer(TimerCallback callback, TimerSpec call, Object callbackObject) {
    return new TimerTask() {
      public void run() {
        // NOP
      }
    };
  }

  public void shutdown() {
    return;
  }

  public Timer getTimer() {
    return null;
  }
}
