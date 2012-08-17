/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import com.tc.test.config.model.TestConfig;

public class ClusteredCacheTTIEventualTest extends AbstractClusteredCacheTTITest {

  public ClusteredCacheTTIEventualTest(TestConfig testConfig) {
    super(testConfig, ClusteredCacheTTITestEventualClient.class);
  }

}
