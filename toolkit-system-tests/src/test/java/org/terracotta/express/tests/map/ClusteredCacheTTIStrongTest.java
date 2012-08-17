/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import com.tc.test.config.model.TestConfig;

public class ClusteredCacheTTIStrongTest extends AbstractClusteredCacheTTITest {

  public ClusteredCacheTTIStrongTest(TestConfig testConfig) {
    super(testConfig, ClusteredCacheTTITestStrongClient.class);
  }

}
