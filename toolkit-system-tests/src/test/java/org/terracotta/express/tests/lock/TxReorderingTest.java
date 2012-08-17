/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class TxReorderingTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 2;

  public TxReorderingTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(TxReorderingClient.class, NODE_COUNT);
  }

}
