/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

import java.util.Date;
import java.util.TimerTask;

/**
 * Interface that mirrors java.util.Timer
 *
 * @author orion
 */
public interface TCTimer {

  public void cancel();

  public void schedule(TimerTask task, long delay);

  public void schedule(TimerTask task, Date time);

  public void schedule(TimerTask task, long delay, long period);

  public void schedule(TimerTask task, Date firstTime, long period);

  public void scheduleAtFixedRate(TimerTask task, long delay, long period);

  public void scheduleAtFixedRate(TimerTask task, Date firstTime,
                                  long period);
}
