/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.nonstop.NonStopNoOpTest.NonStopNoOpTestClient;

import com.tc.test.config.model.TestConfig;

public class NonStopNoOpImmediateTimeoutEnabledTest extends AbstractToolkitTestBase {

  public NonStopNoOpImmediateTimeoutEnabledTest(TestConfig testConfig) {
    super(testConfig, NonStopNoOpImmediateTimeoutEnabledTestClient.class,
          NonStopNoOpImmediateTimeoutEnabledTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopNoOpImmediateTimeoutEnabledTestClient extends NonStopNoOpTestClient {
    public NonStopNoOpImmediateTimeoutEnabledTestClient(String[] args) {
      super(args);
    }

    @Override
    protected boolean isImmediateTimeoutEnabled() {
      return true;
    }
  }

}
