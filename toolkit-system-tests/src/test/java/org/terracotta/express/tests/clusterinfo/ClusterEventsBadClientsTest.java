/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.clusterinfo;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

/**
 * DEV-5051: Whenever a client connects to the cluster or disconnects, corresponding cluster events are fired and the
 * client's client_coordination_stage thread is used for firing these events. The listeners of these events can perform
 * bad operations which can bring down the client's stage thread or get it stuck. So, client need to fire cluster events
 * away from its core context for safety reasons.
 */

public class ClusterEventsBadClientsTest extends AbstractToolkitTestBase {

  public ClusterEventsBadClientsTest(TestConfig testConfig) {
    super(testConfig, ClusterEventsBadClientsTestApp.class, ClusterEventsBadClientsTestApp.class,
          ClusterEventsBadClientsTestApp.class);
    testConfig.getGroupConfig().setMemberCount(2);

    testConfig.getCrashConfig().setCrashMode(ServerCrashMode.RANDOM_ACTIVE_CRASH);
    testConfig.getCrashConfig().setServerCrashWaitTimeInSec(45);
  }

}
