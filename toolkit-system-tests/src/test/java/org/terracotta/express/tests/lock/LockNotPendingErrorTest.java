/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class LockNotPendingErrorTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 2;

  public LockNotPendingErrorTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(LockNotPendingErrorTestClient.class, NODE_COUNT);
  }
}
