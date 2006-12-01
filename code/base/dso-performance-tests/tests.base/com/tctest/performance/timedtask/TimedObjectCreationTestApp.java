/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.timedtask;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.List;

public final class TimedObjectCreationTestApp extends AbstractTransparentApp {

  private static final int    VOLUME   = 100000;
  private static final double NANOSEC  = 1000000000D;
  private final List          rootList = new ArrayList();

  public TimedObjectCreationTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String className = TimedObjectCreationTestApp.class.getName();
    config.addRoot("rootList", className + ".rootList");
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }
  
  public static void visitDSOApplicationConfig(ConfigVisitor visitor, DSOApplicationConfig config) {
    String className = TimedObjectCreationTestApp.class.getName();
    config.addRoot("rootList", className + ".rootList");
    String methodExpression = "* " + className + "*.*(..)";
    config.addWriteAutolock(methodExpression);
  }

  public void run() {
    long start = System.nanoTime();
    for (int i = 0; i < VOLUME; i++) {
      add(new Object());
    }
    long end = System.nanoTime();

    printResult(start, end);
  }

  private void add(Object obj) {
    synchronized (rootList) {
      rootList.add(obj);
    }
  }
  
  private void printResult(long start, long end) {
    double time = (end - start);
    long result = Math.round(VOLUME / (time / NANOSEC));
    System.out.println("**%% TERRACOTTA TEST STATISTICS %%**: value=" + result + " obj/sec");
  }
}
