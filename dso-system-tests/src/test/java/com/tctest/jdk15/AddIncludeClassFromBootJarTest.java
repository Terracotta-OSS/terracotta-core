/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.AddIncludeClassFromBootJarTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

/**
 * This test is mainly to model and test the use case reported in CDV-456. Specifically, the use case is to add ReentrantReadWriteLock.WriteLock
 * to the config with honorTransient to true which will result in NPE when the lock is faulted in the other node. In general, this problem
 * could apply to any class that is in the boot jar.
 *
 */
public class AddIncludeClassFromBootJarTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return AddIncludeClassFromBootJarTestApp.class;
  }

}
