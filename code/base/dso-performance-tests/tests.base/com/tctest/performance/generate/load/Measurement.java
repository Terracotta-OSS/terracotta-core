/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.generate.load;

public final class Measurement {

  public final long x;
  public final long y;

  public Measurement(long x, long y) {
    this.x = x;
    this.y = y;
  }
  
  public String toString() {
    return "x=" + x + " y=" + y; 
  }
}