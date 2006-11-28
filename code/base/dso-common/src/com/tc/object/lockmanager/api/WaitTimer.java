/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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

