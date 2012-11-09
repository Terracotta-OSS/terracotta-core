/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.nonstop;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.toolkit.cache.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.nonstop.NonStopConfigFields.NonStopTimeoutBehavior;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields.Consistency;

import com.tc.test.config.model.TestConfig;

public class NonStopLocalReadTest extends AbstractToolkitTestBase {

  public NonStopLocalReadTest(TestConfig testConfig) {
    super(testConfig, NonStopLocalReadTestClient.class, NonStopLocalReadTestClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  public static class NonStopLocalReadTestClient extends AbstractNonStopTestClient {
    public NonStopLocalReadTestClient(String[] args) {
      super(args);
    }

    @Override
    protected NonStopTimeoutBehavior getTimeoutBehavior() {
      return NonStopTimeoutBehavior.LOCAL_READS;
    }

    @Override
    protected void addMoreConfigToBuilder(ToolkitCacheConfigBuilder builder) {
      builder.consistency(Consistency.STRONG);
    }
  }

}
