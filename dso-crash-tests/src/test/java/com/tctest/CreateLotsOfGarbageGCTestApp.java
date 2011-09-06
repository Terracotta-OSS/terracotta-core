package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tctest.runner.AbstractTransparentApp;

/**
 * Test Application
 */
public class CreateLotsOfGarbageGCTestApp extends AbstractTransparentApp {

  private static final int SIZE  = 50;
  private static int       LOOP_COUNT;

  private final Object[]   array = new Object[SIZE];

  public CreateLotsOfGarbageGCTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    if (Os.isSolaris()) {
      LOOP_COUNT = 2000;
    } else {
      LOOP_COUNT = 3000;
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = CreateLotsOfGarbageGCTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("array", "root");
  }

  public void run() {
    for (int i = 0; i < LOOP_COUNT; i++) {
      synchronized (array) {
        for (int j = 0; j < SIZE; j++) {
          array[j] = new Object();
        }
      }
      if (i != 0 && i % 100 == 0) {
        System.out.println("Loop count : " + i);
        ThreadUtil.reallySleep(1000);
      }
    }
  }
}