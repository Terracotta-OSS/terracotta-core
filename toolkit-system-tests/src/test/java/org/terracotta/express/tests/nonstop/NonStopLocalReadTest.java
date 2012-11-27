/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;

public class NonStopLocalReadTest extends AbstractToolkitTestBase {

  public NonStopLocalReadTest(TestConfig testConfig) {
    super(testConfig, NonStopLocalReadTestClient.class, NonStopLocalReadTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
    testConfig.getL2Config().setRestartable(true);
  }

  public static class NonStopLocalReadTestClient extends AbstractNonStopTestClient {
    public NonStopLocalReadTestClient(String[] args) {
      super(args);
    }

    @Override
    protected NonStopTimeoutBehavior getImmutableOpTimeoutBehavior() {
      return NonStopTimeoutBehavior.LOCAL_READS;
    }

    @Override
    protected NonStopTimeoutBehavior getMutableOpTimeoutBehavior() {
      return NonStopTimeoutBehavior.NO_OP;
    }

    @Override
    protected void addMoreConfigToBuilder(ToolkitCacheConfigBuilder builder) {
      builder.consistency(Consistency.STRONG);
    }
  }

}
