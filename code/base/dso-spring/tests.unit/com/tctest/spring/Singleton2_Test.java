/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.Root;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.Singleton;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Spring singleton test
 */
public class Singleton2_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 4;

  public Singleton2_Test() {
    if (Vm.isIBM()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return SingletonApp.class;
  }

  public static class SingletonApp extends AbstractTransparentApp {
    private List sharedSingletons1 = new ArrayList();
    private List sharedSingletons2 = new ArrayList();

    public SingletonApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      try {
        ClassPathXmlApplicationContext ctx1 = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory.xml");
        ClassPathXmlApplicationContext ctx2 = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory2.xml");

        Singleton singleton1 = (Singleton) ctx1.getBean("singleton");
        Singleton singleton2 = (Singleton) ctx2.getBean("singleton");

        synchronized (sharedSingletons1) {
          sharedSingletons1.add(singleton1);
        }
        synchronized (sharedSingletons2) {
          sharedSingletons2.add(singleton2);
        }

        moveToStageAndWait(1);

        synchronized (sharedSingletons1) {
          // assertTrue("Expected more then one object in sharedSingletons1", sharedSingletons1.size()>1);

          for (Iterator it = sharedSingletons1.iterator(); it.hasNext();) {
            Object o = it.next();
            assertTrue("Found non-singleton object", o == singleton1);
            if (o == singleton2) {
              notifyError("Distinct objects expected");
            }
          }
        }

        synchronized (sharedSingletons2) {
          // assertTrue("Expected more then one object in sharedSingletons2", sharedSingletons2.size()>1);

          for (Iterator it = sharedSingletons2.iterator(); it.hasNext();) {
            Object o = it.next();
            assertTrue("Found non-singleton object", o == singleton2);
            if (o == singleton1) {
              notifyError("Distinct objects expected");
            }
          }
        }

      } catch (Throwable e) {
        moveToStage(1);
        notifyError(e);

      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config
          .addRoot(new Root("com.tctest.spring.Singleton_Test$SingletonApp", "sharedSingletons1", "sharedSingletons1"),
                   false);
      config
          .addRoot(new Root("com.tctest.spring.Singleton_Test$SingletonApp", "sharedSingletons2", "sharedSingletons2"),
                   false);

      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing
      // framework
      springConfig.addConfigPattern("*/beanfactory.xml");
      springConfig.addConfigPattern("*/beanfactory2.xml");
      springConfig.addBean("singleton");
      config.addDSOSpringConfig(springConfig);
    }

  }
}
