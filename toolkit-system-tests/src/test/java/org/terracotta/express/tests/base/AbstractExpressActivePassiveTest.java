/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.base;



import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

public abstract class AbstractExpressActivePassiveTest extends AbstractToolkitTestBase {

  public AbstractExpressActivePassiveTest(TestConfig testConfig, Class<? extends ClientBase>... c) {
    super(testConfig, c);
    testConfig.setNumOfGroups(1);
    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
    testConfig.getCrashConfig().setCrashMode(ServerCrashMode.NO_CRASH);
  }

}
