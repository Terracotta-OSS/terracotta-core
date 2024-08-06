/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TestTimer extends Timer {

  public List<ScheduleCallContext> scheduleCalls = new ArrayList<ScheduleCallContext>();
  public List<Object> cancelCalls   = new ArrayList<Object>();

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
