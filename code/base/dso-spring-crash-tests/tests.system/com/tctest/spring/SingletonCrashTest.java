/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.CrashSingleton;

/**
 * Spring singleton tests
 */
public class SingletonCrashTest extends TransparentTestBase {
  private static final int  NODE_COUNT    = 2;

  private static final long TEST_DURATION = 20L * 1000L;
  private static final long INTERVAL      = 1000L;

  public SingletonCrashTest() {
    // DEV-755 MNK-259
    if (Vm.isIBM()) {
      disableAllUntil("2007-10-01");
    }
    disableAllUntil("2007-07-16"); // MNK-216
  }
  
  protected boolean canRunCrash() {
    return true;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return SingletonApp.class;
  }

  public static class SingletonApp extends AbstractTransparentApp {
    public SingletonApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      long total = TEST_DURATION / INTERVAL;
      long timeout = System.currentTimeMillis() + TEST_DURATION;

      try {
        moveToStageAndWait(1);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
                                                                                "com/tctest/spring/singleton-crash-beanfactory.xml");
        CrashSingleton singleton = (CrashSingleton) ctx.getBean("singleton");

        for (int i = 1; i < total; i++) {
          System.err.println("Loop counter: " + i);

          singleton.incrementCounter();
          moveToStageAndWait(i * 10);
          assertEquals(NODE_COUNT * i, singleton.getCounter());
          moveToStageAndWait(i * 10 + 1);

          try {
            Thread.sleep(INTERVAL);
          } catch (InterruptedException ex) {
          }
        }
        System.err.println("Closing context ... ");
        ctx.close();
        System.err.println("Context closed. ");
      } catch (Throwable e) {
        notifyError(e);
      } finally {
        for (int i = 1; i < total; i++) {
          moveToStageAndWait(i * 10);
          moveToStageAndWait(i * 10 + 1);
        }
      }

      System.err.println("Exiting ... ");
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern("com.tc.object.loaders.IsolationClassLoader"); // app name used by testing
                                                                                            // framework
      springConfig.addConfigPattern("*/singleton-crash-beanfactory.xml");
      springConfig.addBean("singleton");
      config.addDSOSpringConfig(springConfig);
    }
  }
}
