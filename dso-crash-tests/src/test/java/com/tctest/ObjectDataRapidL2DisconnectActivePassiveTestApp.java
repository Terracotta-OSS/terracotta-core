/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

/**
 * DEV-4047: In the AA world, an Object Lookup can happen before the Object actually gets create at the server. Server
 * shouldn't crash on those scenarios
 */
public class ObjectDataRapidL2DisconnectActivePassiveTestApp extends AbstractErrorCatchingTransparentApp {

  private static final int  SIZE        = 10000;
  private static final long TIME_TO_RUN = 10 * 60 * 1000;

  private final Object[]    array       = new Object[SIZE];

  public ObjectDataRapidL2DisconnectActivePassiveTestApp(String appId, ApplicationConfig cfg,
                                                         ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = ObjectDataRapidL2DisconnectActivePassiveTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("array", "array");
  }

  @Override
  public void runTest() {
    final long startTime = System.currentTimeMillis();
    synchronized (array) {
      for (int j = 0; j < SIZE; j++) {
        array[j] = new Object();

        if (j != 0 && j % 100 == 0) {
          System.out.println("Loop count : " + j);
          ThreadUtil.reallySleep(1000);
        }
      }

      // spin cpu so that stage threads for L2ObjectSyncSendHandler gets slow
      // look at DEV-6499 for more details
      while (System.currentTimeMillis() - startTime < TIME_TO_RUN) {
        //
      }
    }
  }

}