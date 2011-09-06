/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.test.ConfigBuilderFactoryImpl;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOApplicationConfigImpl;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.objectserver.storage.util.SetDbClean;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * DEV-2011. For the case, both active and passive go down. But active goes down for good. Passive restores data and
 * becomes active.
 */
public class PassiveClrDirtyDbActivePassiveTest extends ActivePassiveTransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return DummyTestApp.class;
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

  @Override
  protected DSOApplicationConfigBuilder createDsoApplicationConfig() {
    System.out.println("Creating dso app config");
    DSOApplicationConfig cfg = new DSOApplicationConfigImpl(new ConfigBuilderFactoryImpl());
    String testClass = TestApp.class.getName();
    cfg.addIncludePattern(testClass + "*");
    String methodExpression = "* " + testClass + "*.*(..)";
    cfg.addWriteAutolock(methodExpression);

    cfg.addRoot("counter", testClass + ".counter");
    DSOApplicationConfigBuilder dsoAppConfigBuilder = super.createDsoApplicationConfig();
    cfg.writeTo(dsoAppConfigBuilder);
    return dsoAppConfigBuilder;
  }

  public static class DummyTestApp extends AbstractTransparentApp {
    public DummyTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
      super(appId, config, listenerProvider);
    }

    public void run() {
      // Nothing to do.
    }
  }

  public static class TestApp {
    private static final AtomicInteger counter = new AtomicInteger();

    public static void main(String[] args) {
      System.out.println("Value " + inc());
    }

    public static int inc() {
      return counter.incrementAndGet();
    }
  }

  @Override
  protected void customizeActivePassiveTest(final ActivePassiveServerManager manager) throws Exception {
    System.out.println("XXX Start active server[0]");
    manager.startServer(0);
    manager.getAndUpdateActiveIndex();
    Thread.sleep(5000);

    System.out.println("XXX Start passive server[1]");
    manager.startServer(1);
    Thread.sleep(1000);
    Assert.assertTrue(manager.waitServerIsPassiveStandby(1, 600));

    System.out.println("XXX Starting test client");
    File client1Workspace = new File(getTempDirectory(), "l1-logs" + File.separator + "client1");
    client1Workspace.mkdirs();
    ExtraL1ProcessControl client1 = new ExtraL1ProcessControl("localhost", manager.getDsoPort(), TestApp.class,
                                                              getConfigFileLocation(), Collections.EMPTY_LIST,
                                                              client1Workspace, Collections.EMPTY_LIST);

    client1.start();
    client1.waitFor();

    System.out.println("XXX Stop passive server[1]");
    manager.stopServer(1);

    System.out.println("XXX Stop active server[0]");
    manager.stopServer(0);

    // clean up passive dirty db
    System.out.println("XXX Clean passive db dirty bit");
    LinkedJavaProcess setDbCleanProcess = new LinkedJavaProcess(SetDbClean.class.getName(), Arrays.asList("-c", manager
        .getConfigCreator().getDataLocation(1) + File.separator + "objectdb"), getTCPropertyJvmArgs());
    setDbCleanProcess.start();
    setDbCleanProcess.mergeSTDOUT("[SetDbClean] ");
    setDbCleanProcess.mergeSTDERR("[SetDbClean] ");
    System.out.println("XXX SetDbCleanCommand exited with status " + setDbCleanProcess.waitFor());

    System.out.println("XXX Start passive server[1] as active");
    manager.startServer(1);
    Assert.assertEquals(1, manager.getAndUpdateActiveIndex());

    client1.start();
    Assert.assertEquals(0, client1.waitFor());
  }

  private List<String> getTCPropertyJvmArgs() {
    RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
    List<String> tcPropertyDefines = new ArrayList<String>();
    for (String jvmArg : mxbean.getInputArguments()) {
      if (jvmArg.startsWith("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX)) {
        tcPropertyDefines.add(jvmArg);
      }
    }
    return tcPropertyDefines;
  }
}
