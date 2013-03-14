/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Shelestovich
 */
public class TaskRunnerTest {

  private static final String TIMER_NAME = "timer1";

  @Test
  public void testShouldSetCustomNameAndRestoreAfterExecution() throws Exception {
    final TaskRunner runner = Runners.newSingleThreadScheduledTaskRunner();

    final CountDownLatch latch1 = new CountDownLatch(1);
    final Timer timer1 = runner.newTimer(TIMER_NAME);
    timer1.schedule(new Runnable() {
      @Override
      public void run() {
        assertEquals(TIMER_NAME, Thread.currentThread().getName());
        latch1.countDown();
      }
    }, 0, TimeUnit.NANOSECONDS);
    latch1.await(5, TimeUnit.SECONDS);

    final CountDownLatch latch2 = new CountDownLatch(1);
    final Timer timer2 = runner.newTimer();
    timer2.execute(new Runnable() {
      @Override
      public void run() {
        assertTrue(!TIMER_NAME.equals(Thread.currentThread().getName()));
        latch2.countDown();
      }
    });
    latch2.await(5, TimeUnit.SECONDS);
  }

  @Test
  public void testShouldHandleUncaughtException() throws InterruptedException {
    final ThreadFactory factory = new ThreadFactoryBuilder()
        .setDaemon(true).setNameFormat(TIMER_NAME).build();
    final TestTaskRunner runner = new TestTaskRunner(1, factory);
    final CountDownLatch latch = new CountDownLatch(1);

    assertFalse(runner.isExceptionOccured());
    runner.submit(new Runnable() {
      @Override
      public void run() {
        throw new RuntimeException("test-exception");
      }
    });
    latch.await(5, TimeUnit.SECONDS);
    assertTrue(runner.isExceptionOccured());
  }

  @Test(expected = IllegalStateException.class)
  public void testShouldThrowISEIfNewTimerOnShutdown() {
    final TaskRunner runner = Runners.newSingleThreadScheduledTaskRunner();
    runner.shutdown();
    runner.newTimer();
  }

  @Test(expected = IllegalStateException.class)
  public void testShouldThrowISEIfScheduleOnCancelledTimer() {
    final TaskRunner runner = Runners.newSingleThreadScheduledTaskRunner();
    final Timer timer = runner.newTimer();
    Runnable noopTask = new Runnable() {
      @Override
      public void run() {
        // do nothing
      }
    };
    timer.execute(noopTask);
    timer.cancel();
    timer.execute(noopTask);
  }

  private static class TestTaskRunner extends ScheduledNamedTaskRunner {

    private volatile boolean exceptionOccured;

    public TestTaskRunner(final int corePoolSize) {
      super(corePoolSize);
    }

    public TestTaskRunner(final int corePoolSize, final ThreadFactory threadFactory) {
      super(corePoolSize, threadFactory);
    }

    public TestTaskRunner(final int corePoolSize, final RejectedExecutionHandler handler) {
      super(corePoolSize, handler);
    }

    public TestTaskRunner(final int corePoolSize, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
      super(corePoolSize, threadFactory, handler);
    }

    @Override
    protected void handleUncaughtException(final Throwable e) {
      exceptionOccured = true;
    }

    public boolean isExceptionOccured() {
      return exceptionOccured;
    }
  }
}
