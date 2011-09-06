/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class InjectionOnLoadWithMethodOnLoadTest extends TransparentTestBase {

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(3);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return InjectionOnLoadWithMethodOnLoadTestApp.class;
  }
}