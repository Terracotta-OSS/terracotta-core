/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tctest.RemoveObjectsTestApp;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class RemoveObjectsTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 2;

  protected Class getApplicationClass() {
    return RemoveObjectsTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.setGCEnabled(true);
    configFactory.setGCVerbose(true);
    configFactory.setGCIntervalInSec(10);
  }
}
