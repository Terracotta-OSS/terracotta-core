/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import net.sf.ehcache.util.NamedThreadFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NonStopExecutor {
  public final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
      new NamedThreadFactory("NonStopExecutor", true));

  public Future schedule(Runnable task, long timeout) {
    return executor.schedule(task, timeout, TimeUnit.MILLISECONDS);
  }

  public void remove(Future future) {
    if (future instanceof Runnable) {
      executor.remove((Runnable) future);
    }
  }

  public void shutdown() {
    executor.shutdown();
  }

}
