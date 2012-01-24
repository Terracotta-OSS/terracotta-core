/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.ClusterMetaDataTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;

public class ClusterMetaDataTest extends TransparentTestBase {

  public ClusterMetaDataTest() {
    // XXX: This test wants to make some reasonable assertions about values being local
    // XXX: The "builtin" classes don't implement stuff like that.
    // XXX: I think this test should move to toolkit and use a CDM
    timebombTestForRewrite();
  }

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    jvmArgs.add("-Dcom.tc.cachemanager.enable.logging=true");
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(ClusterMetaDataTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return ClusterMetaDataTestApp.class;
  }
}
