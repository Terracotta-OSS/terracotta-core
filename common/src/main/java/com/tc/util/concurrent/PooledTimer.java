/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

import com.google.common.base.Preconditions;
import com.tc.text.StringUtils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Timer} implementation based on a shared thread pool.
 *
 * @author Eugene Shelestovich
 * @see Timer
 * @see ScheduledNamedTaskRunner
 */
class PooledTimer implements Timer {

  private static final String ALREADY_CANCELLED_MSG = "Cannot schedule a task - the timer has been already cancelled";

  private final ScheduledNamedTaskRunner executor;
  private final String name;
  private volatile boolean cancelled;

  public PooledTimer(ScheduledNamedTaskRunner executor) {
    this(null, executor);
  }

  public PooledTimer(String name, ScheduledNamedTaskRunner executor) {
    Preconditions.checkNotNull(executor);

    this.name = name;
    this.executor = executor;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    Preconditions.checkState(!cancelled, ALREADY_CANCELLED_MSG);
    return executor.schedule(wrap(command), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                long period, TimeUnit unit) {
    Preconditions.checkState(!cancelled, ALREADY_CANCELLED_MSG);
    return executor.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                   long delay, TimeUnit unit) {
    Preconditions.checkState(!cancelled, ALREADY_CANCELLED_MSG);
    return executor.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
  }

  @Override
  public void execute(Runnable command) {
    Preconditions.checkState(!cancelled, ALREADY_CANCELLED_MSG);
    executor.execute(wrap(command));
  }

  @Override
  public void cancel() {
    executor.cancelTimer(this);
    cancelled = true;
  }

  private Runnable wrap(Runnable r) {
    return (StringUtils.isNotBlank(name)) ? new TimerNamedRunnable() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Timer getTimer() {
        return PooledTimer.this;
      }

      @Override
      public void run() {
        r.run();
      }
    } : r;
  }
}
