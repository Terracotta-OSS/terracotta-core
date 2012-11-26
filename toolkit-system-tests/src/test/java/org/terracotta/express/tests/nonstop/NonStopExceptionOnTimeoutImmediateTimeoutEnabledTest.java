/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.nonstop.NonStopExceptionOnTimeoutTest.NonStopExceptionOnTimeoutTestClient;

import com.tc.test.config.model.TestConfig;

public class NonStopExceptionOnTimeoutImmediateTimeoutEnabledTest extends AbstractToolkitTestBase {

  public NonStopExceptionOnTimeoutImmediateTimeoutEnabledTest(TestConfig testConfig) {
    super(testConfig, NonStopExceptionOnTimeoutImmediateTimeoutEnabledTestClient.class, NonStopExceptionOnTimeoutImmediateTimeoutEnabledTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopExceptionOnTimeoutImmediateTimeoutEnabledTestClient extends
      NonStopExceptionOnTimeoutTestClient {
    public NonStopExceptionOnTimeoutImmediateTimeoutEnabledTestClient(String[] args) {
      super(args);
    }

    @Override
    protected boolean isImmediateTimeoutEnabled() {
      return true;
    }
  }

}
