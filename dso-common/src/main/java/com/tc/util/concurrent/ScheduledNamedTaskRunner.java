/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
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
 * An executor service to manage scheduled and un-scheduled async tasks. It has several specifal features:
 * <ul>
 * <li>Able to use named threads for tasks. Consider {@link NamedRunnable} for details.</li>
 * <li>Terminates JVM if any thread throws uncaught exception.
 * Override {@link #handleUncaughtException(Throwable)} for custom behavior.</li>
 * </ul>
 *
 * @author Eugene Shelestovich
 * @see NamedRunnable
 * @see ScheduledThreadPoolExecutor
 */
public class ScheduledNamedTaskRunner extends ScheduledThreadPoolExecutor implements TaskRunner {

  private static final TCLogger logger = TCLogging.getLogger(ScheduledNamedTaskRunner.class);

  private final ConcurrentMap<Thread, String> initialThreadNames = new ConcurrentHashMap<Thread, String>();

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
      // remember initial name
      initialThreadNames.putIfAbsent(t, t.getName());
      t.setName(((NamedRunnableScheduledFuture)r).getName());
    }
    super.beforeExecute(t, r);
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
          future.get();
        }
      } catch (CancellationException ce) {
        logger.debug("A task executed by '" + t.getName() + "' thread has been gracefully cancelled");
        // do nothing
      } catch (ExecutionException ee) {
        e = ee.getCause();
      } catch (InterruptedException ie) {
        logger.warn("A task executed by '" + t.getName() + "' thread has been interrupted");
        e = ie;
        //Thread.currentThread().interrupt();
      }
    }

    if (e != null) {
      handleUncaughtException(e);
    }

    if (r instanceof NamedRunnableScheduledFuture) {
      // rollback to initial name
      t.setName(initialThreadNames.remove(t));
    }
  }

  @Override
  protected <V> RunnableScheduledFuture<V> decorateTask(final Runnable runnable,
                                                        final RunnableScheduledFuture<V> task) {
    if (runnable instanceof NamedRunnable) {
      return new NamedRunnableScheduledFuture<V>(((NamedRunnable)runnable).getName(), task);
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

  private static final class ThreadGroupAwareFactory implements ThreadFactory {
    private final ThreadGroup group;

    private ThreadGroupAwareFactory(final ThreadGroup group) {this.group = group;}

    @Override
    public Thread newThread(final Runnable r) {
      return new Thread(group, r);
    }
  }

  private final class NamedRunnableScheduledFuture<V> implements RunnableScheduledFuture<V> {

    private final String name;
    private final RunnableScheduledFuture<V> target;

    private NamedRunnableScheduledFuture(final String name, final RunnableScheduledFuture<V> target) {
      this.name = name;
      this.target = target;
    }

    public String getName() {
      return this.name;
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
