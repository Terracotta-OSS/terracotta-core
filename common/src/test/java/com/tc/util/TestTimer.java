/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
