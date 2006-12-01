/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

public class CrashSingleton {
  private volatile int counter = 0;

  synchronized public int getCounter() {
    return counter;
  }
  
  synchronized public void incrementCounter() {
      this.counter++;
  }
  
  public String toString() {
    return "CrashSingleton:"+counter;
  }
}
