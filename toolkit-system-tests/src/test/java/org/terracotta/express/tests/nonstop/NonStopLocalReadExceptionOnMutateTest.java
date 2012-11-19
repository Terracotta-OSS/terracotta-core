/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;

public class NonStopLocalReadExceptionOnMutateTest extends AbstractToolkitTestBase {

  public NonStopLocalReadExceptionOnMutateTest(TestConfig testConfig) {
    super(testConfig, NonStopLocalReadExceptionOnMutateTestClient.class, NonStopLocalReadExceptionOnMutateTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopLocalReadExceptionOnMutateTestClient extends AbstractNonStopTestClient {
    public NonStopLocalReadExceptionOnMutateTestClient(String[] args) {
      super(args);
    }

    @Override
    protected NonStopTimeoutBehavior getTimeoutBehavior() {
      return NonStopTimeoutBehavior.EXCEPTION_ON_MUTATE_AND_LOCAL_READS;
    }

    @Override
    protected void addMoreConfigToBuilder(ToolkitCacheConfigBuilder builder) {
      builder.consistency(Consistency.STRONG);
    }
  }

}
