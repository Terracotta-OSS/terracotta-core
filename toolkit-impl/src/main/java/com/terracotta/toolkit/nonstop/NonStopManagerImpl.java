/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.internal.nonstop.NonStopManager;

import com.tc.abortable.AbortableOperationManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopManagerImpl implements NonStopManager {
  private final AbortableOperationManager                             abortableOperationManager;
  private final Timer                                                 timer      = new Timer(
                                                                                             "Timer for Non Stop tasks",
                                                                                             true);
  private final ConcurrentMap<Thread, ValueWrapper<NonStopTimerTask>> timerTasks = new ConcurrentHashMap<Thread, ValueWrapper<NonStopTimerTask>>();

  public NonStopManagerImpl(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void begin(long timeout) {
    abortableOperationManager.begin();
    if (timeout <= 0) { return; }
    if (!timerTasks.containsKey(Thread.currentThread())) {
      NonStopTimerTask task = new NonStopTimerTask(Thread.currentThread());
      timerTasks.put(Thread.currentThread(), new ValueWrapper(task));
      timer.schedule(task, timeout);
    } else {
      ValueWrapper valueWrapper = timerTasks.get(Thread.currentThread());
      valueWrapper.increment();
    }
  }

  @Override
  public void finish() {
    ValueWrapper<NonStopTimerTask> valueWrapper = timerTasks.get(Thread.currentThread());
    if (valueWrapper != null) {
      valueWrapper.decrement();
      if (valueWrapper.isZero()) {
        timerTasks.remove(Thread.currentThread());
        valueWrapper.getValue().cancelTaskIfRequired();
      }
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

  private static class ValueWrapper<V> {
    private final V value;
    private int     count;

    public ValueWrapper(V value) {
      this.value = value;
      this.count = 1;
    }

    private void increment() {
      count++;
    }

    private void decrement() {
      count--;
    }

    public V getValue() {
      return value;
    }

    private boolean isZero() {
      return count == 0;
    }
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
