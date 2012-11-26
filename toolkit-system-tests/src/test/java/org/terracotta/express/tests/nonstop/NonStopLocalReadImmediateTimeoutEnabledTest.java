/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.nonstop.NonStopLocalReadTest.NonStopLocalReadTestClient;

import com.tc.test.config.model.TestConfig;

public class NonStopLocalReadImmediateTimeoutEnabledTest extends AbstractToolkitTestBase {

  public NonStopLocalReadImmediateTimeoutEnabledTest(TestConfig testConfig) {
    super(testConfig, NonStopLocalReadImmediateTimeoutEnabledTestClient.class,
          NonStopLocalReadImmediateTimeoutEnabledTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopLocalReadImmediateTimeoutEnabledTestClient extends NonStopLocalReadTestClient {
    public NonStopLocalReadImmediateTimeoutEnabledTestClient(String[] args) {
      super(args);
    }

    @Override
    protected boolean isImmediateTimeoutEnabled() {
      return true;
    }
  }

}
