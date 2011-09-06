/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.gcrunner.GCRunner;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

public class MutateDGCValidateTestApp extends ServerCrashingAppBase {
  private final ServerControl       serverControl;
  public static List<Integer> rootList;
  private static final int    OBJECT_COUNT = 5000;

  public MutateDGCValidateTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    serverControl = config.getServerControl();
  }

  @Override
  protected void runTest() throws Throwable {
    rootList = new ArrayList<Integer>();
    ExtraL1ProcessControl client = spawnNewClientAndWaitForCompletion("1", WorkOnList.class);
    Assert.assertNotNull(client);

    performDGC();

    ThreadUtil.reallySleep(2000);

    crashServer();
    startServer();

    validate();
    System.out.println("Test finished.");
  }

  private void performDGC() {
    GCRunner runner = new GCRunner(getHostName(), getAdminPort());
    try {
      runner.runGC();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void validate() {
    Assert.assertEquals(OBJECT_COUNT, rootList.size());
    for (int i = 0; i < OBJECT_COUNT; i++) {
      Assert.assertEquals(i, (rootList.get(i)).intValue());
    }
  }

  private void crashServer() {
    System.out.println("XXX crashing the server...");
    try {
      serverControl.crash();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("XXX server crashed...");
  }

  private void startServer() {
    System.out.println("XXX starting the server...");
    try {
      serverControl.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("XXX server started...");
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = MutateDGCValidateTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);

    config.addIncludePattern(MutateDGCValidateTestApp.class.getName());
    config.addIncludePattern(WorkOnList.class.getName());

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    methodExpression = "* " + WorkOnList.class.getName() + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("rootList", "rootList");
  }

  public static class WorkOnList {
    public static void main(String[] args) {
      addToSharedList();
    }

    private static void addToSharedList() {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (int i = 0; i < OBJECT_COUNT * 2; i++) {
        list.add(new Integer(i));
      }

      synchronized (MutateDGCValidateTestApp.rootList) {
        for (int i = 0; i < OBJECT_COUNT; i++) {
          MutateDGCValidateTestApp.rootList.add(list.get(i));
        }
      }
    }
  }
}
