/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

public class ReadWriteLockCrashTest extends AbstractToolkitTestBase {

  private final int globalParticipantCount = 5;

  public ReadWriteLockCrashTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(ReadWriteLockCrashTestClient.class, globalParticipantCount);
    testConfig.getCrashConfig().setCrashMode(ServerCrashMode.RANDOM_ACTIVE_CRASH);
    testConfig.getCrashConfig().setServerCrashWaitTimeInSec(90);
    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getL2Config().setDgcIntervalInSec(10);
    testConfig.getL2Config().setDgcEnabled(true);
  }

}
