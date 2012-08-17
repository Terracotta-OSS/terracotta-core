/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;

import com.tc.test.config.model.TestConfig;

public class BlockingQueueMultiThreadTest extends AbstractExpressActivePassiveTest {
  public BlockingQueueMultiThreadTest(TestConfig testConfig) {
    super(testConfig, BlockingQueueMultiThreadTestClient.class, BlockingQueueMultiThreadTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

}
