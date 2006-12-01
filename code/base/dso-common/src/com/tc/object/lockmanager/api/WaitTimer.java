/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.object.tx.WaitInvocation;

import java.util.Timer;
import java.util.TimerTask;


public interface WaitTimer {
  
  public TimerTask scheduleTimer(WaitTimerCallback callback, WaitInvocation call, Object callbackObject);
  
  public void shutdown();

  public Timer getTimer();

}

