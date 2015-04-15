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
package com.tc.util.concurrent;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An executor service to manage scheduled and un-scheduled async tasks. It has several special features:
 * <ul>
 * <li>Able to use named threads for tasks. Consider {@link Timer} for details.</li>
 * <li>Terminates JVM if any thread throws uncaught exception.
 * Override {@link #handleUncaughtException(Throwable)} for custom behavior.</li>
 * <li>Maintains a set of timers. A {@link Timer} is a short-lived view object which users should use to schedule tasks.</li>
 * </ul>
 *
 * @author Eugene Shelestovich
 * @see Timer
 * @see Runners
 */
public class ScheduledNamedTaskRunner extends ScheduledThreadPoolExecutor implements TaskRunner {

  private static final TCLogger logger = TCLogging.getLogger(ScheduledNamedTaskRunner.class);
  // to restore the initial thread's name after a task execution
  private final ThreadLocal<String> initialThreadName = new ThreadLocal<String>();
  // to be able to cancel all tasks related to a particular timer
  private final Map<Timer, Set<Future<?>>> timerTasks = new HashMap<Timer, Set<Future<?>>>();

  public ScheduledNamedTaskRunner(final int corePoolSize) {
    super(corePoolSize);
  }

  public ScheduledNamedTaskRunner(final int corePoolSize, final ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
  }

  public ScheduledNamedTaskRunner(final int corePoolSize, final RejectedExecutionHandler handler) {
    super(corePoolSize, handler);
  }

  public ScheduledNamedTaskRunner(final int corePoolSize, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
    super(corePoolSize, threadFactory, handler);
  }

  public ScheduledNamedTaskRunner(final int corePoolSize, final ThreadGroup threadGroup) {
    super(corePoolSize, newThreadFactory(threadGroup));
  }

  protected void handleUncaughtException(final Throwable e) {
    final Thread t = Thread.currentThread();
    // honors existing Thread's uncaught exception handler
    t.getUncaughtExceptionHandler().uncaughtException(t, e);
  }

  @Override
  protected void beforeExecute(final Thread t, final Runnable r) {
    if (r instanceof NamedRunnableScheduledFuture) {
      final NamedRunnableScheduledFuture namedRunnable = (NamedRunnableScheduledFuture)r;
      // remember initial name
      if (StringUtils.isNotBlank(namedRunnable.getName())) {
        initialThreadName.set(t.getName());
        t.setName(namedRunnable.getName());
      }
      // register a new timer task
      registerTimerTask(namedRunnable);
    }
    super.beforeExecute(t, r);
  }

  private synchronized void registerTimerTask(final NamedRunnableScheduledFuture namedRunnable) {
    final Timer timer = namedRunnable.getTimer();
    Set<Future<?>> tasks = timerTasks.get(timer);
    if (tasks == null) {
      tasks = new HashSet<Future<?>>();
      timerTasks.put(timer, tasks);
    }
    tasks.add(namedRunnable);
  }

  private synchronized void unregisterTimerTask(final NamedRunnableScheduledFuture namedRunnable) {
    final Timer timer = namedRunnable.getTimer();
    final Set<Future<?>> tasks = timerTasks.get(timer);
    if (tasks != null) {
      tasks.remove(namedRunnable);
      if (tasks.isEmpty()) {
        timerTasks.remove(timer);
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p/>Since {@link FutureTask} maintains exceptions internally, and {@link Thread.UncaughtExceptionHandler} is not called,
   * we need to manually invoke the handler.
   */
  @Override
  protected void afterExecute(Runnable r, Throwable e) {
    super.afterExecute(r, e);
    final Thread t = Thread.currentThread();

    if (e == null && r instanceof Future<?>) {
      try {
        Future<?> future = (Future<?>)r;
        if (future.isDone()) {
          uninterruptedGet(future);
        }
      } catch (CancellationException ce) {
        logger.debug("A task executed by '" + t.getName() + "' thread has been gracefully cancelled");
        // do nothing
      } catch (ExecutionException ee) {
        e = ee.getCause();
      }
    }

    if (e != null) {
      handleUncaughtException(e);
    }

    if (r instanceof NamedRunnableScheduledFuture) {
      final NamedRunnableScheduledFuture namedRunnable = (NamedRunnableScheduledFuture)r;
      // rollback to initial thread's name
      if (StringUtils.isNotBlank(namedRunnable.getName())) {
        t.setName(initialThreadName.get());
        initialThreadName.remove();
      }
      // cleanup successfully executed task
      unregisterTimerTask(namedRunnable);
    }
  }

  private static void uninterruptedGet(final Future<?> future) throws ExecutionException {
    while (true) {
      try {
        future.get();
        break;
      } catch (InterruptedException ie) {
        logger.debug("A task executed by '" + Thread.currentThread().getName() + "' thread has been interrupted", ie);
        // clear interrupted flag and try to get the result again
      }
    }
  }

  @Override
  protected <V> RunnableScheduledFuture<V> decorateTask(final Runnable runnable,
                                                        final RunnableScheduledFuture<V> task) {
    if (runnable instanceof TimerNamedRunnable) {
      final TimerNamedRunnable namedRunnable = (TimerNamedRunnable)runnable;
      return new NamedRunnableScheduledFuture<V>(namedRunnable, task);
    } else {
      return super.decorateTask(runnable, task);
    }
  }

  private static ThreadFactory newThreadFactory(final ThreadGroup threadGroup) {
    final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
    if (threadGroup != null) {
      builder.setThreadFactory(new ThreadGroupAwareFactory(threadGroup));
    }
    builder.setDaemon(true).setNameFormat("task-runner-thread-%s").setPriority(Thread.NORM_PRIORITY);
    return builder.build();
  }

  @Override
  public Timer newTimer() {
    return newTimer(null);
  }

  @Override
  public Timer newTimer(final String name) {
    Preconditions.checkState(!isShutdown(), "Cannot create a timer - the parent task runner has been already shut down");
    return new PooledTimer(name, this);
  }

  @Override
  public synchronized void cancelTimer(final Timer timer) {
    final Set<Future<?>> tasks = timerTasks.remove(timer);
    if (tasks != null) {
      // cancel all tasks
      for (Future<?> task : tasks) {
        task.cancel(false);
      }
      // cleanup executor's queue
      purge();
    }
  }

  @Override
  public void shutdown() {
    super.shutdownNow();

    boolean timedout = false;
    try {
      timedout = !awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (timedout) {
        logger.warn("Not all TaskRunner threads gracefully finished execution");
      }
    }

    initialThreadName.remove();
    synchronized (this) {
      timerTasks.clear();
    }
  }

  private static final class ThreadGroupAwareFactory implements ThreadFactory {
    private final ThreadGroup group;

    private ThreadGroupAwareFactory(final ThreadGroup group) {this.group = group;}

    @Override
    public Thread newThread(final Runnable r) {
      return new Thread(group, r);
    }
  }

  private final class NamedRunnableScheduledFuture<V> implements RunnableScheduledFuture<V>, TimerNamedRunnable {

    private final TimerNamedRunnable namedRunnable;
    private final RunnableScheduledFuture<V> target;

    private NamedRunnableScheduledFuture(final TimerNamedRunnable namedRunnable,
                                         final RunnableScheduledFuture<V> target) {
      this.namedRunnable = namedRunnable;
      this.target = target;
    }

    @Override
    public String getName() {
      return namedRunnable.getName();
    }

    @Override
    public Timer getTimer() {
      return namedRunnable.getTimer();
    }

    @Override
    public boolean isPeriodic() {
      return target.isPeriodic();
    }

    @Override
    public long getDelay(final TimeUnit unit) {
      return target.getDelay(unit);
    }

    @Override
    public int compareTo(final Delayed o) {
      return target.compareTo(o);
    }

    @Override
    public void run() {
      target.run();
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
      return target.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return target.isCancelled();
    }

    @Override
    public boolean isDone() {
      return target.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
      return target.get();
    }

    @Override
    public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return target.get(timeout, unit);
    }

  }

}
