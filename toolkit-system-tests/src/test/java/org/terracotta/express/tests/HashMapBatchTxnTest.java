/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

/*
 * Test case for CDV-253
 */

public class HashMapBatchTxnTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 2;

  public HashMapBatchTxnTest(TestConfig config) {
    super(config);
    config.getClientConfig().setClientClasses(HashMapBatchTxnTestClient.class, NODE_COUNT);
    config.getClientConfig().setMaxHeap(256);
  }

}
