/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import com.tc.test.config.model.TestConfig;

public class ClusteredCacheTTICrashEventualTest extends AbstractClusteredCacheTTICrashTest {

  public ClusteredCacheTTICrashEventualTest(TestConfig testConfig) {
    super(testConfig, ClusteredCacheTTICrashTestEventualClient.class);
  }

}
