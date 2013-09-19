/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

import com.google.common.base.Preconditions;
import com.tc.properties.TCPropertiesImpl;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An utility class similar to {@link Executors}.
 * Contains factory methods for most popular {@link TaskRunner} configurations.
 */
public final class Runners {

  private Runners() {
  }

  public static TaskRunner newDefaultCachedScheduledTaskRunner() {
    return newDefaultCachedScheduledTaskRunner(null);
  }

  public static TaskRunner newDefaultCachedScheduledTaskRunner(final ThreadGroup threadGroup) {
    final ScheduledNamedTaskRunner runner = new ScheduledNamedTaskRunner(TCPropertiesImpl.getProperties()
        .getInt("default.taskrunner.core.pool.size", 16), threadGroup);
    runner.setKeepAliveTime(2L, TimeUnit.MINUTES); // automatically shrinks after some idle period
    runner.allowCoreThreadTimeOut(true); // allow removing core pool threads
    //runner.setRemoveOnCancelPolicy(true); // JDK 1.7 only
    return runner;
  }

  public static TaskRunner newScheduledTaskRunner(int poolSize) {
    return newScheduledTaskRunner(poolSize, null);
  }

  public static TaskRunner newScheduledTaskRunner(int poolSize, final ThreadGroup threadGroup) {
    Preconditions.checkArgument(poolSize > 0, "poolSize should be a positive integer");

    return new ScheduledNamedTaskRunner(poolSize, threadGroup);
  }

  public static TaskRunner newSingleThreadScheduledTaskRunner() {
    return newScheduledTaskRunner(1);
  }

  public static TaskRunner newSingleThreadScheduledTaskRunner(final ThreadGroup threadGroup) {
    return newScheduledTaskRunner(1, threadGroup);
  }

}
