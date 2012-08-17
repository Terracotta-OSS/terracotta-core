/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class FastReadSlowWriteTest extends AbstractToolkitTestBase {

  public FastReadSlowWriteTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(FastReadSlowWriteTestClient.class,
                                                  FastReadSlowWriteTestClient.NODE_COUNT);
    testConfig.getClientConfig().setMaxHeap(80);
    testConfig.getClientConfig().setMinHeap(80);
    testConfig.getL2Config().setMaxHeap(200);
    testConfig.getL2Config().setMinHeap(200);
    disableIfMemoryLowerThan(1);
  }

}
