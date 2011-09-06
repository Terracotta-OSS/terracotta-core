/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TestTimer extends Timer {

  public List scheduleCalls = new ArrayList();
  public List cancelCalls   = new ArrayList();

  @Override
  public void cancel() {
    cancelCalls.add(new Object());
    super.cancel();
  }

  @Override
  public void schedule(TimerTask task, long delay) {
    scheduleCalls.add(new ScheduleCallContext(task, Long.valueOf(delay), null, null));
    super.schedule(task, delay);
  }

  @Override
  public void schedule(TimerTask task, long delay, long period) {
    scheduleCalls.add(new ScheduleCallContext(task, Long.valueOf(delay), null, Long.valueOf(period)));
    super.schedule(task, delay, period);
  }

  public static final class ScheduleCallContext {
    public final TimerTask task;
    public final Long      delay;
    public final Date      time;
    public final Long      period;

    private ScheduleCallContext(TimerTask task, Long delay, Date time, Long period) {
      this.task = task;
      this.delay = delay;
      this.time = time;
      this.period = period;
    }
  }

}
