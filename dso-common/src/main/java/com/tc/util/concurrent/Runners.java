/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * An utility class similar to {@link Executors}.
 * Contains factory methods for most popular {@link TaskRunner} configurations.
 */
public final class Runners {

  private Runners() {
  }

  public static TaskRunner newCachedScheduledTaskRunner() {
    return newCachedScheduledTaskRunner(null);
  }

  public static TaskRunner newCachedScheduledTaskRunner(final ThreadGroup group) {
    final ScheduledNamedTaskRunner runner = new ScheduledNamedTaskRunner(16, newThreadFactory(group));
    runner.setKeepAliveTime(5L, TimeUnit.MINUTES); // automatically shrinks after 5 min being idle
    runner.allowCoreThreadTimeOut(true); // allow removing core pool threads
    //runner.setRemoveOnCancelPolicy(true); // JDK 1.7 only
    return runner;
  }

  public static TaskRunner newScheduledTaskRunner(int poolSize) {
    return newScheduledTaskRunner(poolSize, null);
  }

  public static TaskRunner newScheduledTaskRunner(int poolSize, final ThreadGroup group) {
    Preconditions.checkArgument(poolSize > 0, "poolSize should be a positive integer");

    return new ScheduledNamedTaskRunner(poolSize, newThreadFactory(group));
  }

  public static TaskRunner newSingleThreadScheduledTaskRunner() {
    return newScheduledTaskRunner(1);
  }

  public static TaskRunner newSingleThreadScheduledTaskRunner(final ThreadGroup group) {
    return newScheduledTaskRunner(1, group);
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
}
