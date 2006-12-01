/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import EDU.oswego.cs.dl.util.concurrent.Latch;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A wrapper around java.util.Timer. Someone might add more it more in the future, but for now all I'm adding is a way
 * to set the name of the background execution thread so that it can be identified properly in a thread dump
 */
public class TCTimerImpl extends Timer implements TCTimer {

  // NOTE: There isn't a cstr with a default for isDaemon on purpose. Best to think about this and be explicit
  public TCTimerImpl(final String threadName, boolean isDaemon) {
    super(isDaemon);

    final Latch proceed = new Latch();

    TimerTask nameSetter = new TimerTask() {
      public void run() {
        Thread.currentThread().setName(threadName);
        proceed.release();
      }
    };

    schedule(nameSetter, 0L);

    try {
      proceed.acquire();
    } catch (InterruptedException e) {
      // ignore
    }
  }

}
