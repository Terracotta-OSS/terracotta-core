/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.BaseDSOTestCase;

/*
 * Unit test for measuring the overhead of StringBuffer for jdk1.5.
 */
public class StringBufferPerformance15Test extends BaseDSOTestCase {
  private final static int                COUNT                 = 100000;

  public void test() {
    StringBuffer sb = new StringBuffer();
    char[] appendString = new char[]{'t', 'h', 'i', 's', ' ', 'i', 's', ' ', 'a', ' ', 't', 'e', 's', 't'};
    long startTime = System.currentTimeMillis();
    for (int i=0; i<COUNT; i++) {
      sb.append(appendString);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Time elapsed for non-shared StringBuffer: " + (endTime - startTime) + " ms.");
  }
 
}
