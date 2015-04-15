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

import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogging;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ThrottledTaskExecutorTest extends TCTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TCLogging.getLogger(ThrottledTaskExecutor.class).setLevel(LogLevelImpl.DEBUG);
  }

  public void testLinearExecute() {
    testBody(1, 5);
  }

  public void testDoubleExecute() {
    testBody(2, 5);
  }

  public void testManyExecute() {
    testBody(10, 5);
  }

  public void testManyExecuteExtended() {
    // add more tasks dynamically during the test run
  }

  public void testBody(int maxTasksToRunParallel, int totalTasksToRun) {

    final AtomicLong appCounter = new AtomicLong(0);
    final AtomicLong finishedCounter = new AtomicLong(0);
    final Object lock = new Object();
    final ThrottledTaskExecutor taskExecutor = new ThrottledTaskExecutor(maxTasksToRunParallel);

    final Runnable appFeedbackRunnable = new Runnable() {
      @Override
      public void run() {
        taskExecutor.receiveFeedback();
        finishedCounter.incrementAndGet();
        synchronized (lock) {
          lock.notify();
        }
      }
    };

    Runnable app = new Runnable() {
      @Override
      public void run() {
        long id = appCounter.incrementAndGet();
        AppTask appTask = new AppTask(id, appFeedbackRunnable);
        long pid = taskExecutor.offer(appTask);
        if (pid > 0) {
          System.err.println("XXX App " + id + " completed, pid = " + pid);
        } else {
          System.err.println("XXX App " + id + " scheduled, pid = " + pid);
        }
      }
    };

    for (int i = 0; i < totalTasksToRun; i++) {
      new Thread(app).start();
    }

    AppVerifierTask verifier = new AppVerifierTask(taskExecutor, maxTasksToRunParallel, totalTasksToRun);
    new Thread(verifier).start();

    synchronized (lock) {
      while (finishedCounter.get() < totalTasksToRun) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          //
        }
      }
    }
    verifier.shutdown.set(true);

    System.err.println("XXX SUCCESS");
  }

  static class AppTask implements Runnable {

    private final long          id;
    private final Runnable      feedbackRunnable;
    private static final Random r = new Random();

    public AppTask(long id, Runnable feedbackRunnable) {
      this.id = id;
      this.feedbackRunnable = feedbackRunnable;
    }

    @Override
    public void run() {
      System.err.println("XXX App Task - " + id + " - START");
      ThreadUtil.reallySleep(1000 + r.nextInt(10000));
      System.err.println("XXX App Task " + id + " - FEEDBACK");
      this.feedbackRunnable.run();
      System.err.println("XXX App Task " + id + " - END");
    }

  }

  static class AppVerifierTask implements Runnable {

    private final ThrottledTaskExecutor throttledTaskExecutor;
    private final int                   maxScheduledTasks;
    private final AtomicBoolean         shutdown = new AtomicBoolean(false);

    public AppVerifierTask(ThrottledTaskExecutor executor, int maxTasksToRunParallel, int totalTasksToRun) {
      this.throttledTaskExecutor = executor;
      this.maxScheduledTasks = (totalTasksToRun - maxTasksToRunParallel) > 0 ? (totalTasksToRun - maxTasksToRunParallel)
          : 0;
    }

    @Override
    public void run() {
      while (!shutdown.get()) {
        int scheduledCount = throttledTaskExecutor.getScheduledTasksCount();
        System.err.println("XXX ScheduledCount: " + scheduledCount + "; Max: " + maxScheduledTasks);
        Assert.eval(throttledTaskExecutor.getScheduledTasksCount() + " vs " + maxScheduledTasks,
                    throttledTaskExecutor.getScheduledTasksCount() <= maxScheduledTasks);
        ThreadUtil.reallySleep(2000);
      }
    }
  }
}
