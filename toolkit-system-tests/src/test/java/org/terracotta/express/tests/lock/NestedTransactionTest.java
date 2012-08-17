/*
 * zzz * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class NestedTransactionTest extends AbstractToolkitTestBase {

  public NestedTransactionTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(NestedTransactionTestClient.class,
                                                  NestedTransactionTestClient.NODE_COUNT);
  }

}
