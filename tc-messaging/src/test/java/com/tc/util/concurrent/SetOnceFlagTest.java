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

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test cases for SetOnceFlag
 * 
 * @author teck
 */
public class SetOnceFlagTest {

  @Test
  public void testRace() throws InterruptedException {
    final Random random = new Random();

    for (int i = 0; i < 50; i++) {
      final SetOnceFlag flag = new SetOnceFlag();
      final AtomicBoolean thread1 = new AtomicBoolean(false);
      final AtomicBoolean thread2 = new AtomicBoolean(false);
      final AtomicReference<Exception> exception = new AtomicReference<Exception>();

      Runnable r1 = new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(random.nextInt(50));
          } catch (InterruptedException e) {
            exception.set(e);
          }

          try {
            flag.set();
            thread1.set(true); // I win!
          } catch (IllegalStateException iae) {
            // I didn't win ;-(
          }
        }
      };

      Runnable r2 = new Runnable() {
        @Override
        public void run() {
          try {
            try {
              Thread.sleep(random.nextInt(50));
            } catch (InterruptedException e) {
              exception.set(e);
            }

            flag.set();
            thread2.set(true); // I win!
          } catch (IllegalStateException iae) {
            // I didn't win ;-(
          }
        }
      };

      if (exception.get() != null) {
        fail("One of threads caught interrupted exception");
      }

      Thread t1 = new Thread(r1);
      Thread t2 = new Thread(r2);
      t1.start();
      t2.start();
      t1.join();
      t2.join();

      assertTrue(thread1.get() ^ thread2.get());
    }
  }

  @Test
  public void testAttemptSet() {
    SetOnceFlag flag = new SetOnceFlag();

    flag.set();

    assertFalse(flag.attemptSet());
  }

  @Test
  public void testMultiSet() {
    SetOnceFlag flag = new SetOnceFlag();

    // set it once
    flag.set();

    // try setting it many more times
    for (int i = 0; i < 100; i++) {
      try {
        flag.set();
        fail();
      } catch (IllegalStateException iae) {
        // expected
      }
    }
  }

  @Test
  public void testMultiRead() {
    SetOnceFlag flag = new SetOnceFlag();

    // set it once
    flag.set();

    // try reading it many times
    for (int i = 0; i < 100; i++) {
      assertTrue(flag.isSet());
    }
  }

  @Test
  public void testInitSet() {
    SetOnceFlag flag = new SetOnceFlag(true);

    try {
      flag.set();
      fail();
    } catch (IllegalStateException iae) {
      // expected
    }
  }

}
