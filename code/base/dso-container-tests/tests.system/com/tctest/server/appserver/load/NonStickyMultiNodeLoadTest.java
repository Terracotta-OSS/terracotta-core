/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

public class NonStickyMultiNodeLoadTest extends MultiNodeLoadTest {

  public void testFourNodeLoad() throws Throwable {
    runFourNodeLoad(false);
  }

}
