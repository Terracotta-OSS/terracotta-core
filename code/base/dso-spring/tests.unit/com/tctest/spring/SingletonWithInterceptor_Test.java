/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.FooService;

/**
 * Spring singleton test
 */
public class SingletonWithInterceptor_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;

  public SingletonWithInterceptor_Test() {
    if (Vm.isIBM()) {
      this.disableAllUntil("2007-10-01");
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
    public SingletonApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      Thread t = Thread.currentThread();
      ClassLoader cl = t.getContextClassLoader();

      try {
        t.setContextClassLoader(getClass().getClassLoader());

        ClassPathXmlApplicationContext ctx1 = new ClassPathXmlApplicationContext(
                                                                                 "com/tctest/spring/beanfactory-interceptor.xml");

        FooService singleton1 = (FooService) ctx1.getBean("service");

        assertEquals("interceptorInvoked", singleton1.serviceMethod());

      } catch (Throwable e) {
        notifyError(e);

      } finally {
        t.setContextClassLoader(cl);

      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing
                                                                                    // framework
      springConfig.addConfigPattern("*/beanfactory-interceptor.xml");
      springConfig.addBean("service");
      config.addDSOSpringConfig(springConfig);

      // config.addIncludePattern("com.tctest.spring.bean.SimpleListener", true, true);
      // config.addIncludePattern("com.tctest.spring.bean.FooServiceImpl", true, true);

      // config.addIsolatedClass("org.springframework.");
      // config.addIsolatedClass("com.tctest.spring.");
      // config.addIsolatedClass("com.tcspring.");
    }

  }
}
