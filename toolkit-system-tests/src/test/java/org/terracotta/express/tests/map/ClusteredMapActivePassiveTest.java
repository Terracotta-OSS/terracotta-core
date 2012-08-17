/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.map;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;

import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

public class ClusteredMapActivePassiveTest extends AbstractExpressActivePassiveTest {
  public ClusteredMapActivePassiveTest(TestConfig testConfig) {
    super(testConfig, ClusteredMapBasicEventualTestClient.class, ClusteredMapBasicEventualTestClient.class);
    testConfig.getCrashConfig().setCrashMode(ServerCrashMode.RANDOM_ACTIVE_CRASH);
    testConfig.getCrashConfig().setServerCrashWaitTimeInSec(30);
  }

}
