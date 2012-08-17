/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.system.tests;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.WaitUtil;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.setup.GroupsData;

import java.io.File;
import java.util.concurrent.Callable;

public class OverlappingDataDirectoryNetworkedHATest extends AbstractToolkitTestBase {

  public OverlappingDataDirectoryNetworkedHATest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
    testConfig.getGroupConfig().setMemberCount(2);
  }

  @Override
  protected void startServers() throws Exception {
    // Start servers in the startClients() method
  }

  @Override
  protected void startClients() throws Throwable {
    ConfigHelper configHelper = new CustomConfigHelper(getGroupsData(), getTestConfig(), getTcConfigFile(),
                                                       getTempDirectory());
    configHelper.writeConfigFile();
    System.out.println("Starting up the active");
    testServerManager.startServer(0, 0);

    System.out.println("Starting up the passive with the active's data directory");
    testServerManager.startServerNoWait(0, 1);

    System.out.println("Waiting 30s for the passive to attempt to start up.");
    SECONDS.sleep(30);

    WaitUtil.waitUntilCallableReturnsFalse(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return testServerManager.isPassiveStandBy(0);
      }
    });

    System.out.println("Cleaning up servers");
    testServerManager.stopAllServers();
  }

  private class CustomConfigHelper extends ConfigHelper {
    public CustomConfigHelper(GroupsData[] groupData, TestConfig testConfig, File tcConfigFile, File tempDir) {
      super(groupData, testConfig, tcConfigFile, tempDir);
    }

    @Override
    protected String getDataDirectoryPath(int groupIndex, int serverIndex) {
      // Force all servers in the group to map to the same data directory
      return super.getDataDirectoryPath(groupIndex, 0);
    }

  }
}
