/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.concurrent;

import org.slf4j.LoggerFactory;


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
                                                                 LoggerFactory.getLogger(ThreadPreferenceExecutorTest.class));
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

  public void testThreadReuse() {
    ThreadPreferenceExecutor exec = new ThreadPreferenceExecutor("test", 10, 5, TimeUnit.SECONDS,
                                                                 LoggerFactory
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
