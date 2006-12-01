/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;


/**
 * @author steve
 */
public class TransparencySpeedTest  extends TransparentTestBase implements TestConfigurator {

  private int clientCount = TransparencySpeedTestApp.MUTATOR_COUNT + TransparencySpeedTestApp.VERIFIER_COUNT;

  protected Class getApplicationClass() {
    return TransparencySpeedTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setApplicationInstancePerClientCount(1).setIntensity(1);
    t.initializeTestRunner();
  }

}
