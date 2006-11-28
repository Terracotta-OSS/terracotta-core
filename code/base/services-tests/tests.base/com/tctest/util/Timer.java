/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.util;

public class Timer
{
  public long start;
  public long end;
  
  public void start()
  {
    start = System.currentTimeMillis();
  }
  
  public void stop()
  {
    end = System.currentTimeMillis();
  }
  
  public long elapsed()
  {
    return end - start;
  }
  
  public float tps(long t)
  {
    return (float) t / elapsed() * 1000;
  }
}
