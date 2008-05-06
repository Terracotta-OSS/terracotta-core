/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.test.TCTestCase;
import com.tc.util.runtime.Vm;

public class MonitorUtilsTest extends TCTestCase {

  public void test() throws Exception {
    // this test is Sun VM only, of course this condition only exludes JRockit, but that ought to do it for now
    if (Vm.isJRockit()) {
      System.err.println("THIS TEST WORKS ONLY ON SUN VM");
      return;
    }

    Thread.currentThread().setName("main");
    final Latch latch = new Latch();
    final Latch latch2 = new Latch();
    final Object lock = new Object();
    final SynchronizedRef ref = new SynchronizedRef(null);

    Thread t = new Thread() {
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
                latch.release();
                log("about to sleep");
                ThreadUtil.reallySleep(10000);
                log("done sleeping, about to release monitor");
                int count = MonitorUtils.releaseMonitor(lock);
                log("release count was " + count);
                latch2.acquire();
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
    latch.acquire();
    log("signaled");

    synchronized (lock) {
      log("got the lock");
      latch2.release();
      log("released");
    }

    log("joining thread");
    t.join();
    log("thread dead");

    if (ref.get() != null) {
      fail((Throwable) ref.get());
    }

  }

  void log(String msg) {
    System.out.println(System.currentTimeMillis() + " [" + Thread.currentThread().getName() + "] " + msg);
  }

}
