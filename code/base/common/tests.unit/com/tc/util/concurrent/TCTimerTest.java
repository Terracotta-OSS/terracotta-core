/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.util.TCTimer;
import com.tc.util.TCTimerImpl;

import java.util.TimerTask;

import junit.framework.TestCase;

public class TCTimerTest extends TestCase {

  public void test() throws InterruptedException {
    final String name = "testing 1 2 3";
    TCTimer timer = new TCTimerImpl(name, true);

    final Latch proceed = new Latch();
    final SynchronizedRef actualName = new SynchronizedRef(null);

    TimerTask task = new TimerTask() {
      public void run() {
        actualName.set(Thread.currentThread().getName());
        proceed.release();
      }
    };

    timer.schedule(task, 0L);
    proceed.acquire();

    assertEquals(name, actualName.get());

    timer.cancel();
  }

}
