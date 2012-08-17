/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class RecallUnderConcurrentLockTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 2;

  public RecallUnderConcurrentLockTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(RecallUnderConcurrentLockTestClient.class, NODE_COUNT);
  }

}
