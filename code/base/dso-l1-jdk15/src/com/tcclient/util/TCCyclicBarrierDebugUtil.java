/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import com.tc.util.DebugUtil;

import java.util.concurrent.CyclicBarrier;

public class TCCyclicBarrierDebugUtil {
  public static synchronized int acquire(CyclicBarrier barrier, int participants, boolean startDebug, boolean endDebug) throws Exception {
    int numWaiting = barrier.getNumberWaiting();
    if ((numWaiting == 0) && startDebug){
      DebugUtil.DEBUG = true;
    }
    int returnValue = barrier.await();
    if ((numWaiting == (participants -1)) && endDebug) {
      DebugUtil.DEBUG = false;
    }
    return returnValue;
  }
  
  public static int acquire(CyclicBarrier barrier, int participants) throws Exception {
    return acquire(barrier, participants, false, false);
  }
}
