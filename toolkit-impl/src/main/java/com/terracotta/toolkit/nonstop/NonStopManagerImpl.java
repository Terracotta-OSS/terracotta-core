/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopManagerImpl implements NonStopManager {
  private final AbortableOperationManager               abortableOperationManager;
  private final Timer                                   timer      = new Timer("Timer for Non Stop tasks", true);
  private final ConcurrentMap<Thread, NonStopTimerTask> timerTasks = new ConcurrentHashMap<Thread, NonStopTimerTask>();

  public NonStopManagerImpl(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void begin(long timeout) {
    abortableOperationManager.begin();
    NonStopTimerTask task = new NonStopTimerTask(Thread.currentThread());
    timerTasks.put(Thread.currentThread(), task);
    timer.schedule(task, timeout);
  }

  @Override
  public void finish() {
    NonStopTimerTask task = timerTasks.remove(Thread.currentThread());
    if (task != null) {
      task.cancelTaskIfRequired();
    }
    abortableOperationManager.finish();
  }

  @Override
  public void shutdown() {
    timer.cancel();
  }

  private enum NonStopTimerTaskState {
    INIT, ABORTED, CANCELLED
  }

  private class NonStopTimerTask extends TimerTask {
    private final Thread          thread;
    private NonStopTimerTaskState state = NonStopTimerTaskState.INIT;

    public NonStopTimerTask(Thread thread) {
      this.thread = thread;
    }

    public synchronized void cancelTaskIfRequired() {
      if (state == NonStopTimerTaskState.INIT) {
        state = NonStopTimerTaskState.CANCELLED;
        this.cancel();
      }
    }

    @Override
    public synchronized void run() {
      if (state == NonStopTimerTaskState.INIT) {
        state = NonStopTimerTaskState.ABORTED;
        abortableOperationManager.abort(thread);
      }
    }
  }
}
