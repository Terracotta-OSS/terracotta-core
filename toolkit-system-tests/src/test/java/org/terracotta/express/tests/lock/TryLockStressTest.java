/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class TryLockStressTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 3;

  public TryLockStressTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(TryLockStressTestClient.class, NODE_COUNT);
  }

}