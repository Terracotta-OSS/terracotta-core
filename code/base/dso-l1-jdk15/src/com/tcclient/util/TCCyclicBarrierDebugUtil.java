/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcclient.util;

import java.util.concurrent.CyclicBarrier;

public class TCCyclicBarrierDebugUtil {
  public static synchronized int acquire(CyclicBarrier barrier, int participants, boolean startDebug, boolean endDebug)
      throws Exception {
    return barrier.await();
  }

  public static int acquire(CyclicBarrier barrier, int participants) throws Exception {
    return acquire(barrier, participants, false, false);
  }
}
