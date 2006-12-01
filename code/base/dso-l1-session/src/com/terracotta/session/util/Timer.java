/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

public class Timer {
  private long start = 0;
  private long end   = 0;

  public Timer() {
    start();
  }

  public Timer(boolean doStart) {
    start = (doStart) ? System.currentTimeMillis() : 0;
  }

  public void start() {
    start = System.currentTimeMillis();
  }

  public void stop() {
    end = System.currentTimeMillis();
  }

  public long elapsed() {
    return end - start;
  }

  public void reset() {
    start = end = 0;
  }

  public long getEnd() {
    return end;
  }

  public long getStart() {
    return start;
  }
}
