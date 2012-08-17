/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;

import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;

import java.io.File;
import java.util.Arrays;

public class PassiveSmoothStartTest extends AbstractExpressActivePassiveTest {

  public PassiveSmoothStartTest(TestConfig testConfig) {
    super(testConfig, PassiveSmoothStartTestApp.class);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
    testConfig.getCrashConfig().setShouldCleanDbOnCrash(false);
  }

  @Override
  protected void runClient(Class client) throws Throwable {
    String server1DataLocation = new File(tempDir, "testserver0" + File.separator + "data").getAbsolutePath();
    String server2DataLocation = new File(tempDir, "testserver1" + File.separator + "data").getAbsolutePath();

    runClient(PassiveSmoothStartTestApp.class, PassiveSmoothStartTestApp.class.getSimpleName(),
              Arrays.asList(server1DataLocation, server2DataLocation));
  }
}
