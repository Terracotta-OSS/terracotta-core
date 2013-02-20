/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import java.util.Timer;
import java.util.TimerTask;

public class NonStopTimer {
  private static final long TIMER_PURGE_TIME = Long
                                                 .getLong("com.terracotta.toolkit.nonstop.time.to.purge.milliseconds",
                                                          15000);

  private final Timer       timer1           = new Timer("NonStopTimer timer 1", true);
  private final Timer       timer2           = new Timer("NonStopTimer timer 2", true);

  private volatile Timer    timer            = timer1;

  public NonStopTimer() {
    timer1.schedule(new TimerTask() {
      @Override
      public void run() {
        Timer oldTimer = timer;
        timer = timer == timer1 ? timer2 : timer1;

        oldTimer.purge();
      }
    }, TIMER_PURGE_TIME, TIMER_PURGE_TIME);
  }

  public void schedule(TimerTask task, long timeout) {
    timer.schedule(task, timeout);
  }

  public void cancel() {
    timer1.cancel();
    timer2.cancel();
  }

}
