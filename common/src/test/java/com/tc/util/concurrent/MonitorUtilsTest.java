/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.test.TCTestCase;
import com.tc.util.runtime.Vm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class MonitorUtilsTest extends TCTestCase {

  public void test() throws Exception {
    // this test is Sun VM only, of course this condition only exludes JRockit, but that ought to do it for now
    if (Vm.isJRockit()) {
      System.err.println("THIS TEST WORKS ONLY ON SUN VM");
      return;
    }

    Thread.currentThread().setName("main");
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch latch2 = new CountDownLatch(1);
    final Object lock = new Object();
    final AtomicReference<Throwable> ref = new AtomicReference<Throwable>(null);

    Thread t = new Thread() {
      @Override
      public void run() {
        setName("thread");
        log("started");
        try {
          synchronized (lock) {
            log("monitor acquired 1");
            synchronized (lock) {
              log("monitor acquired 2");
              synchronized (lock) {
                log("monitor acquired 3");
                latch.countDown();
                log("about to sleep");
                ThreadUtil.reallySleep(10000);
                log("done sleeping, about to release monitor");
                int count = MonitorUtils.releaseMonitor(lock);
                log("release count was " + count);
                latch2.await();
                log("re-acquiring lock");
                MonitorUtils.monitorEnter(lock, count);
                log("lock re-acquired");
              }
              log("left block 1");
            }
            log("left block 2");
          }
          log("left block 3");
        } catch (Throwable ex) {
          ref.set(ex);
        }
      }
    };
    t.start();

    log("waiting for thread");
    latch.await();
    log("signaled");

    synchronized (lock) {
      log("got the lock");
      latch2.countDown();
      log("released");
    }

    log("joining thread");
    t.join();
    log("thread dead");

    if (ref.get() != null) {
      fail(ref.get());
    }

  }

  void log(String msg) {
    System.out.println(System.currentTimeMillis() + " [" + Thread.currentThread().getName() + "] " + msg);
  }

}
