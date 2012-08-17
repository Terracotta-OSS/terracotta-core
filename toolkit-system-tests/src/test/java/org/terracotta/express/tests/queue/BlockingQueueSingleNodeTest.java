/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;

import com.tc.test.config.model.TestConfig;

public class BlockingQueueSingleNodeTest extends AbstractExpressActivePassiveTest {
  public BlockingQueueSingleNodeTest(TestConfig testConfig) {
    super(testConfig, BlockingQueueSingleNodeTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

}
