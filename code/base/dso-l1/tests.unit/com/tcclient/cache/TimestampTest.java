/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.cache;

import java.util.Calendar;

import junit.framework.TestCase;

public class TimestampTest extends TestCase {

  private long startTime() {
    Calendar cal = Calendar.getInstance();
    cal.set(0, 1, 107);
    return cal.getTimeInMillis();
  }
  
  public void test() {
    long start = startTime();
    long maxIdle = 10*1000;
    long maxTTL = 20*1000;
    Timestamp t = new Timestamp(start, maxIdle, maxTTL);
    assertEquals(start+maxIdle, t.getInvalidatedTimeMillis());
    assertEquals(start+maxIdle, t.getExpiredTimeMillis());
  }
  
}
