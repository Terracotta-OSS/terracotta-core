/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;

public class CacheLoadPerformanceTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  public void setUp() throws Exception {
    super.setUp();

    ((SettableConfigItem) configFactory().l2DSOConfig().garbageCollectionInterval()).setValue(1000000);

    getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);

    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return CacheLoadPerformanceTestApp.class;
  }

}
