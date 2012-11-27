/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.nonstop.NonStopLocalReadExceptionOnMutateTest.NonStopLocalReadExceptionOnMutateTestClient;

import com.tc.test.config.model.TestConfig;

public class NonStopLocalReadExceptionOnMutateImmediateTimeoutEnabledTest extends AbstractToolkitTestBase {

  public NonStopLocalReadExceptionOnMutateImmediateTimeoutEnabledTest(TestConfig testConfig) {
    super(testConfig, NonStopLocalReadExceptionOnMutateTestImmediateTimeoutEnabledClient.class, NonStopLocalReadExceptionOnMutateTestImmediateTimeoutEnabledClient.class);
    testConfig.getClientConfig().setParallelClients(true);
    testConfig.getL2Config().setRestartable(true);
  }

  public static class NonStopLocalReadExceptionOnMutateTestImmediateTimeoutEnabledClient extends
      NonStopLocalReadExceptionOnMutateTestClient {
    public NonStopLocalReadExceptionOnMutateTestImmediateTimeoutEnabledClient(String[] args) {
      super(args);
    }

    @Override
    protected boolean isImmediateTimeoutEnabled() {
      return true;
    }
  }

}
