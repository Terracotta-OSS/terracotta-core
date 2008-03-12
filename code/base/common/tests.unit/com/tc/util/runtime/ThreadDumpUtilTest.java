/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.util.Assert;

import junit.framework.TestCase;

public class ThreadDumpUtilTest extends TestCase {

  public void testThreadDump() {
    String st = ThreadDumpUtil.getThreadDump();
    System.out.println(st);
    Assert.eval("Thread dump should only be supported in jdk 1.5+", st.indexOf("Full thread dump ") < 0);
  }
}
