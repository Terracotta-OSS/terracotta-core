/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.nonstop.NonStopConfigFields.NonStopTimeoutBehavior;

import com.tc.test.config.model.TestConfig;

public class NonStopExceptionOnTimeoutTest extends AbstractToolkitTestBase {

  public NonStopExceptionOnTimeoutTest(TestConfig testConfig) {
    super(testConfig, NonStopExceptionOnTimeoutTestClient.class, NonStopExceptionOnTimeoutTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopExceptionOnTimeoutTestClient extends AbstractNonStopTestClient {
    public NonStopExceptionOnTimeoutTestClient(String[] args) {
      super(args);
    }

    @Override
    protected NonStopTimeoutBehavior getTimeoutBehavior() {
      return NonStopTimeoutBehavior.EXCEPTION_ON_TIMEOUT;
    }

    @Override
    protected void addToLocalCache(ToolkitCache<Integer, Integer> cache) {
      //
    }
  }

}
