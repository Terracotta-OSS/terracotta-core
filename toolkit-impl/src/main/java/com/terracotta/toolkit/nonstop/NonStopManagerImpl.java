/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopManagerImpl implements NonStopManager {
  private static final TCLogger                         LOGGER     = TCLogging.getLogger(NonStopManagerImpl.class);
  private final AbortableOperationManager               abortableOperationManager;
  private final Timer                                   timer      = new Timer("Timer for Non Stop tasks", true);
  private final ConcurrentMap<Thread, NonStopTimerTask> timerTasks = new ConcurrentHashMap<Thread, NonStopTimerTask>();

  public NonStopManagerImpl(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void begin(long timeout) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("begin nonstop operation with timeout :" + timeout + "for Thread : " + Thread.currentThread());
    }
    if (!timerTasks.containsKey(Thread.currentThread())) {
      abortableOperationManager.begin();
      NonStopTimerTask task = new NonStopTimerTask(Thread.currentThread());
      timerTasks.put(Thread.currentThread(), task);
      // Do not start timer for negative timeouts.
      if (timeout > 0) {
        timer.schedule(task, timeout);
      }
    } else {
      throw new IllegalStateException("The thread has already called begin");
    }
  }

  @Override
  public boolean tryBegin(long timeout) {
    if (timerTasks.containsKey(Thread.currentThread())) {
      // Nonstop operation already running
      return false;
    } else {
      begin(timeout);
      return true;
    }

  }

  @Override
  public void finish() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("finish nonstop operation for Thread : " + Thread.currentThread());
    }
    NonStopTimerTask task = timerTasks.get(Thread.currentThread());
    if (task != null) {
      timerTasks.remove(Thread.currentThread());
      task.cancelTaskIfRequired();
      abortableOperationManager.finish();
    } else {
      throw new IllegalStateException("The thread has not called begin");
    }
  }

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

    public synchronized boolean cancelTaskIfRequired() {
      if (state == NonStopTimerTaskState.INIT) {
        state = NonStopTimerTaskState.CANCELLED;
        this.cancel();
        return true;
      } else {
        return false;
      }
    }

    @Override
    public synchronized void run() {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(" nonstop operation with timedout for Thread : " + Thread.currentThread());
      }
      if (state == NonStopTimerTaskState.INIT) {
        state = NonStopTimerTaskState.ABORTED;
        abortableOperationManager.abort(thread);
      }
    }
  }
}
