/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Collections;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopManagerImpl implements NonStopManager {
  private static final TCLogger                         LOGGER     = TCLogging.getLogger(NonStopManagerImpl.class);
  private final AbortableOperationManager               abortableOperationManager;
  private final NonStopTimer                            timer      = new NonStopTimer();
  private final ConcurrentMap<Thread, NonStopTimerTask> timerTasks = new ConcurrentHashMap<Thread, NonStopTimerTask>();

  public NonStopManagerImpl(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void begin(long timeout) {
    if (!timerTasks.containsKey(Thread.currentThread())) {
      abortableOperationManager.begin();
      NonStopTimerTask task = new NonStopTimerTask(Thread.currentThread(), abortableOperationManager);
      timerTasks.put(Thread.currentThread(), task);
      // Do not start timer for negative timeouts.
      if (timeout > 0 && (timeout + System.currentTimeMillis()) > 0) {
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

  Map getTimerTasks() {
    return Collections.unmodifiableMap(timerTasks);
  }

  @Override
  public void finish() {
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

  private static enum NonStopTimerTaskState {
    INIT, ABORTED, CANCELLED
  }

  private static class NonStopTimerTask extends TimerTask {
    private final Thread                    thread;
    private NonStopTimerTaskState           state = NonStopTimerTaskState.INIT;
    private final AbortableOperationManager abortableOperationManager;

    public NonStopTimerTask(Thread thread, AbortableOperationManager abortableOperationManager) {
      this.thread = thread;
      this.abortableOperationManager = abortableOperationManager;
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
        LOGGER.debug("Nonstop operation timed-out for Thread : " + Thread.currentThread());
      }
      if (state == NonStopTimerTaskState.INIT) {
        state = NonStopTimerTaskState.ABORTED;
        abortableOperationManager.abort(thread);
      }
    }
  }
}
