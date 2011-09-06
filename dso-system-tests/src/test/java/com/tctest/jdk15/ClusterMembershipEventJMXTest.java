/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.ClusterMembershipEventJMXTestApp;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ClusterMembershipEventJMXTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return ClusterMembershipEventJMXTestApp.class;
  }

}
