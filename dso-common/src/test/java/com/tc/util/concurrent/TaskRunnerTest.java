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

import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
  public void testShouldHandleUncaughtException() throws InterruptedException, TimeoutException, ExecutionException {
    final ThreadFactory factory = new ThreadFactoryBuilder()
        .setDaemon(true).setNameFormat(TIMER_NAME).build();
    final TestTaskRunner runner = new TestTaskRunner(1, factory);
    assertFalse(runner.isExceptionOccured());

    final Future<?> result = runner.submit(new Runnable() {
      @Override
      public void run() {
        throw new TestException();
      }
    });

    try {
      result.get(5, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof TestException);
    } catch (TimeoutException e) {
      fail("Timeout waiting for the result from task");
    }

    // wait until unhandled exception handler invoked
    while (!runner.isExceptionOccured()) {
      Thread.sleep(50L);
    }
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

  @Test
  public void testShouldNotLoseTaskResultOnInterrupt() throws InterruptedException, TimeoutException, ExecutionException {
    final TestTaskRunner runner = new TestTaskRunner(1);
    assertFalse(runner.isExceptionOccured());

    final Future<?> result = runner.submit(new Runnable() {
      @Override
      public void run() {
        Thread.currentThread().interrupt();
        // we still must get this exception no matter what the thread was interrupted
        throw new TestException();
      }
    });

    try {
      result.get(5, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof TestException);
    } catch (TimeoutException e) {
      fail("Timeout waiting for the result from task");
    }

    // wait until unhandled exception handler invoked
    while (!runner.isExceptionOccured()) {
      Thread.sleep(50L);
    }
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
      if (e instanceof TestException) {
        exceptionOccured = true;
      }
    }

    public boolean isExceptionOccured() {
      return exceptionOccured;
    }
  }

  private static final class TestException extends RuntimeException {

    public TestException() {
    }

    public TestException(final String message) {
      super(message);
    }

    public TestException(final String message, final Throwable cause) {
      super(message, cause);
    }

    public TestException(final Throwable cause) {
      super(cause);
    }
  }
}
