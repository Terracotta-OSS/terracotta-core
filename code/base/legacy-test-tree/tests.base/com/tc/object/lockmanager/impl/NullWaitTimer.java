/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.lockmanager.api.WaitTimer;
import com.tc.object.lockmanager.api.WaitTimerCallback;
import com.tc.object.tx.WaitInvocation;

import java.util.Timer;
import java.util.TimerTask;

public class NullWaitTimer implements WaitTimer {

  public TimerTask scheduleTimer(WaitTimerCallback callback, WaitInvocation call, Object callbackObject) {
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
