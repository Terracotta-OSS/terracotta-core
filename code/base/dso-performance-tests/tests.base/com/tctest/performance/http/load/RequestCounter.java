/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.http.load;

class RequestCounter {
  
  private int count;
  private static final long TIMEOUT = 60 * 1000;

  public synchronized void increment() {
    count++;
    notify();
  }

  public synchronized void waitForCount(int target) throws InterruptedException {
    long time = System.currentTimeMillis() + TIMEOUT;
    while (count < target && System.currentTimeMillis() < time) wait();
    if (System.currentTimeMillis() >= time) System.out.println("Request Timeout");
    count = 0;
  }
}
