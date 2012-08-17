/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class ClusterMembershipEventJMXTest extends AbstractToolkitTestBase {

  private static final int NODE_COUNT = 1;

  public ClusterMembershipEventJMXTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(ClusterMembershipEventJMXTestClient.class, NODE_COUNT);
  }

}
