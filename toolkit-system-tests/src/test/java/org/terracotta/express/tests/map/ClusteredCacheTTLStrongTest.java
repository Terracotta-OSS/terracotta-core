/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import com.tc.test.config.model.TestConfig;

public class ClusteredCacheTTLStrongTest extends AbstractClusteredCacheTTLTest {

  public ClusteredCacheTTLStrongTest(TestConfig testConfig) {
    super(testConfig, ClusteredCacheTTLTestStrongClient.class);
  }

}
