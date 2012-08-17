/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class WaitForAllCurrentTransactionsToCompleteTest extends AbstractToolkitTestBase {

  public WaitForAllCurrentTransactionsToCompleteTest(TestConfig testConfig) {
    super(testConfig, WaitForAllCurrentTransactionsToCompleteTestApp.class,
          WaitForAllCurrentTransactionsToCompleteTestApp.class, WaitForAllCurrentTransactionsToCompleteTestApp.class,
          WaitForAllCurrentTransactionsToCompleteTestApp.class);
  }

}
