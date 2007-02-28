/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.HashMap;
import java.util.Map;

public class SynchronousWriteObjectDataRestartTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 2;

  protected Class getApplicationClass() {
    return ObjectDataRestartTestApp.class;
  }

  protected Map getOptionalAttributes() {
    Map attributes = new HashMap();
    attributes.put(ObjectDataRestartTestApp.SYNCHRONOUS_WRITE, "true");
    return attributes;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  protected boolean canRunCrash() {
    return true;
  }

  protected boolean canRunRestart() {
    return true;
  }

}
