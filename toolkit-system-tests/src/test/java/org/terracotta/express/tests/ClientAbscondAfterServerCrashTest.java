/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;

public class ClientAbscondAfterServerCrashTest extends AbstractToolkitTestBase {

  public ClientAbscondAfterServerCrashTest(TestConfig testConfig) {
    super(testConfig, ClientAbscondAfterServerCrashTestClient.class, ClientAbscondAfterServerCrashTestClient.class);
    testConfig.getGroupConfig().setMemberCount(1);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
    TestBaseUtil.enableL1Reconnect(testConfig);
    testConfig.getL2Config().setClientReconnectWindow(15);
  }

}
