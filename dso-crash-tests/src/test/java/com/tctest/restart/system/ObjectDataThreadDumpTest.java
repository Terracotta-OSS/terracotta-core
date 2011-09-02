/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.TransparentAppConfig;

public class ObjectDataThreadDumpTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 10;

  protected Class<ObjectDataThreadDumpTestApp> getApplicationClass() {
    return ObjectDataThreadDumpTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    int adminPort = getAdminPort();
    cfg.setAttribute(ObjectDataThreadDumpTestApp.JMX_PORT, String.valueOf(adminPort));
  }

}
