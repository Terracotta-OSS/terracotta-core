/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.util.concurrent;

import org.junit.Ignore;
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

  private static final String TASK_NAME = "test-task";
  private static final String THREAD_NAME = "single-thread";

  @Test
  public void testShouldSetCustomNameAndRestoreAfterExecution() throws InterruptedException {
    final ThreadFactory factory = new ThreadFactoryBuilder()
        .setDaemon(true).setNameFormat(THREAD_NAME).build();

    final TaskRunner runner = new ScheduledNamedTaskRunner(1, factory);
    final CountDownLatch latch = new CountDownLatch(3);

    // initial name
    runner.execute(new Runnable() {
      @Override
      public void run() {
        assertEquals(THREAD_NAME, Thread.currentThread().getName());
        latch.countDown();
      }
    });

    // inherited task name
    runner.execute(new NamedRunnable() {
      @Override
      public String getName() {
        return TASK_NAME;
      }

      @Override
      public void run() {
        assertEquals(TASK_NAME, Thread.currentThread().getName());
        latch.countDown();
      }
    });

    // back to initial name
    runner.execute(new Runnable() {
      @Override
      public void run() {
        assertEquals(THREAD_NAME, Thread.currentThread().getName());
        latch.countDown();
      }
    });

    latch.await(5, TimeUnit.SECONDS);
  }

  @Test
  public void testShouldHandleUncaughtException() throws InterruptedException {
    final ThreadFactory factory = new ThreadFactoryBuilder()
        .setDaemon(true).setNameFormat(THREAD_NAME).build();
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
