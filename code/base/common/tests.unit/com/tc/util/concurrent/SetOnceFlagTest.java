/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import java.util.Random;

import junit.framework.TestCase;

/**
 * Test cases for SetOnceFlag
 * 
 * @author teck
 */
public class SetOnceFlagTest extends TestCase {

  public void testRace() throws InterruptedException {
    final Random random = new Random();

    for (int i = 0; i < 50; i++) {
      final SetOnceFlag flag = new SetOnceFlag();
      final SynchronizedBoolean thread1 = new SynchronizedBoolean(false);
      final SynchronizedBoolean thread2 = new SynchronizedBoolean(false);

      Runnable r1 = new Runnable() {
        public void run() {
          try {
            Thread.sleep(random.nextInt(50));
          } catch (InterruptedException e) {
            fail();
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
        public void run() {
          try {
            try {
              Thread.sleep(random.nextInt(50));
            } catch (InterruptedException e) {
              fail();
            }

            flag.set();
            thread2.set(true); // I win!
          } catch (IllegalStateException iae) {
            // I didn't win ;-(
          }
        }
      };

      Thread t1 = new Thread(r1);
      Thread t2 = new Thread(r2);
      t1.start();
      t2.start();
      t1.join();
      t2.join();

      System.out.println("The winner is thread " + (thread1.get() ? "1" : "2"));

      assertTrue(thread1.get() ^ thread2.get());
    }
  }

  public void testAttemptSet() {
    SetOnceFlag flag = new SetOnceFlag();

    flag.set();

    assertFalse(flag.attemptSet());
  }

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

  public void testMultiRead() {
    SetOnceFlag flag = new SetOnceFlag();

    // set it once
    flag.set();

    // try reading it many times
    for (int i = 0; i < 100; i++) {
      assertTrue(flag.isSet());
    }
  }

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
