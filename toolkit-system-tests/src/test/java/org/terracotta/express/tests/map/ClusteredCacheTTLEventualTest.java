/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import com.tc.test.config.model.TestConfig;

public class ClusteredCacheTTLEventualTest extends AbstractClusteredCacheTTLTest {

  public ClusteredCacheTTLEventualTest(TestConfig testConfig) {
    super(testConfig, ClusteredCacheTTLTestEventualClient.class);
  }

}
