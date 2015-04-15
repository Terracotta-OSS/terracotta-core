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
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

public class NonStopManagerImpl implements NonStopManager {
  private static final TCLogger                           LOGGER   = TCLogging.getLogger(NonStopManagerImpl.class);
  private final AbortableOperationManager                 abortableOperationManager;
  private final NonStopExecutor                           executor = new NonStopExecutor();
  private final ConcurrentMap<Thread, NonStopTaskWrapper> tasks    = new ConcurrentHashMap<Thread, NonStopTaskWrapper>();

  public NonStopManagerImpl(AbortableOperationManager abortableOperationManager) {
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void begin(long timeout) {
    if (!tasks.containsKey(Thread.currentThread())) {
      abortableOperationManager.begin();
      NonStopTask task = new NonStopTask(Thread.currentThread(), abortableOperationManager);
      // Do not start timer for negative timeouts.
      Future future = null;
      if (timeout > 0 && (timeout + System.currentTimeMillis()) > 0) {
        future = executor.schedule(task, timeout);
      }
      tasks.put(Thread.currentThread(), new NonStopTaskWrapper(task, future));
    } else {
      throw new IllegalStateException("The thread has already called begin");
    }
  }

  @Override
  public boolean tryBegin(long timeout) {
    if (tasks.containsKey(Thread.currentThread())) {
      // Nonstop operation already running
      return false;
    } else {
      begin(timeout);
      return true;
    }

  }

  Map<Thread, NonStopTaskWrapper> getTimerTasks() {
    return Collections.unmodifiableMap(tasks);
  }

  @Override
  public void finish() {
    NonStopTaskWrapper wrapper = tasks.get(Thread.currentThread());
    if (wrapper != null) {
      tasks.remove(Thread.currentThread());
      wrapper.getTask().cancelTaskIfRequired();
      if (wrapper.getFuture() != null) {
        // this will remove the task from the queue of the executor
        executor.remove(wrapper.getFuture());
      }
      abortableOperationManager.finish();
    } else {
      throw new IllegalStateException("The thread has not called begin");
    }
  }

  public void shutdown() {
    executor.shutdown();
  }

  private static enum NonStopTaskState {
    INIT, ABORTED, CANCELLED
  }

  static class NonStopTaskWrapper {
    private final NonStopTask task;
    private final Future           future;

    public NonStopTaskWrapper(NonStopTask task, Future future) {
      this.task = task;
      this.future = future;
    }

    public NonStopTask getTask() {
      return task;
    }

    public Future getFuture() {
      return future;
    }

  }

  private static class NonStopTask implements Runnable {
    private final Thread                    thread;
    private NonStopTaskState                state = NonStopTaskState.INIT;
    private final AbortableOperationManager abortableOperationManager;

    public NonStopTask(Thread thread, AbortableOperationManager abortableOperationManager) {
      this.thread = thread;
      this.abortableOperationManager = abortableOperationManager;
    }

    public synchronized boolean cancelTaskIfRequired() {
      if (state == NonStopTaskState.INIT) {
        state = NonStopTaskState.CANCELLED;
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
      if (state == NonStopTaskState.INIT) {
        state = NonStopTaskState.ABORTED;
        abortableOperationManager.abort(thread);
      }
    }
  }
}
