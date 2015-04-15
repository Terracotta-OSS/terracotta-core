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

import com.tc.logging.LogLevel;
import com.tc.logging.TCAppender;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class ThreadPreferenceExecutorTest extends TestCase {

  public void testBasic() {
    ThreadPreferenceExecutor exec = new ThreadPreferenceExecutor("test", 10, 5, TimeUnit.SECONDS,
                                                                 TCLogging
                                                                     .getLogger(ThreadPreferenceExecutorTest.class));
    assertEquals(0, exec.getActiveThreadCount());

    final AtomicInteger run = new AtomicInteger();

    for (int i = 0; i < 10; i++) {
      exec.execute(new Runnable() {
        @Override
        public void run() {
          ThreadUtil.reallySleep(5000);
          run.incrementAndGet();
        }
      });

      assertEquals(i + 1, exec.getActiveThreadCount());
    }

    try {
      exec.execute(new Runnable() {
        @Override
        public void run() {
          throw new AssertionError();
        }
      });
    } catch (RejectedExecutionException re) {
      // expected
    }

    ThreadUtil.reallySleep(10000);

    // make sure all tasks complete
    assertEquals(10, run.get());

    ThreadUtil.reallySleep(10000);

    // make sure all threads die
    assertEquals(0, exec.getActiveThreadCount());
  }

  public void testThreadCountLogging() {
    TCLogger logger = TCLogging.getLogger(ThreadPreferenceExecutorTest.class);
    LogAppender logAppender = new LogAppender();
    TCLogging.addAppender(logger.getName(), logAppender);
    ThreadPreferenceExecutor exec = new ThreadPreferenceExecutor("test", 20, 100, TimeUnit.MILLISECONDS, logger);
    assertEquals(0, exec.getActiveThreadCount());

    final AtomicInteger run = new AtomicInteger();

    Runnable longClient = new Runnable() {
      @Override
      public void run() {
        ThreadUtil.reallySleep(10000);
        run.incrementAndGet();
      }
    };

    Runnable shortClient = new Runnable() {
      @Override
      public void run() {
        run.incrementAndGet();
      }
    };

    for (int i = 0; i < 16; i++) {
      exec.execute(longClient);
      assertEquals(i + 1, exec.getActiveThreadCount());
    }

    while (exec.getActiveThreadCount() != 0) {
      ThreadUtil.reallySleep(1000);
    }

    for (int i = 0; i < 10; i++) {
      exec.execute(shortClient);
    }

    while (exec.getActiveThreadCount() != 0) {
      ThreadUtil.reallySleep(1000);
    }

    assertEquals(1, logAppender.getThreadCountLogging());

    // make sure all tasks complete
    assertEquals(26, run.get());

    ThreadUtil.reallySleep(10000);

    // make sure all threads die
    assertEquals(0, exec.getActiveThreadCount());
  }

  private static class LogAppender implements TCAppender {
    private int threadCountLogging = 0;

    @Override
    public synchronized void append(LogLevel level, Object message, Throwable throwable) {
      System.out.println("XXX " + message);
      if (message.toString().contains("thread count")) {
        threadCountLogging++;
      }
    }

    public synchronized int getThreadCountLogging() {
      return threadCountLogging;
    }

  }

  public void testThreadReuse() {
    ThreadPreferenceExecutor exec = new ThreadPreferenceExecutor("test", 10, 5, TimeUnit.SECONDS,
                                                                 TCLogging
                                                                     .getLogger(ThreadPreferenceExecutorTest.class));

    final Set<Thread> threads = Collections.synchronizedSet(new HashSet<Thread>());

    for (int i = 0; i < 10; i++) {
      exec.execute(new Runnable() {
        @Override
        public void run() {
          threads.add(Thread.currentThread());
        }
      });
      ThreadUtil.reallySleep(1000);
      assertEquals(1, exec.getActiveThreadCount());
    }

    assertEquals(1, threads.size());
  }
}
