/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
