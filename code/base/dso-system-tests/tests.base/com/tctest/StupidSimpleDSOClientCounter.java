/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

public class StupidSimpleDSOClientCounter {
  
  private int count;
  
  public int getCount() {
    return this.count;
  }
  
  public void incrementCount() {
    this.count++;
  }
  
  public String toString() {
    return getClass().getName() + ".count= " + this.count;
  }
}
