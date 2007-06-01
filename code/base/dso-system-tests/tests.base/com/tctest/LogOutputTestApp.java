/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

public class LogOutputTestApp extends AbstractErrorCatchingTransparentApp {
  private static final TCLogger logger     = TCLogging.getTestingLogger(LogOutputTestApp.class);
  private static final String   message    = "LogOutputTest:  abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz";
  private static final int      LOOP_COUNT = 1000000;

  public LogOutputTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  protected void runTest() throws Throwable {
    for (int i = 0; i < LOOP_COUNT; i++) {
      logger.info(message);
      // System.out.println(message);
    }
  }

}
