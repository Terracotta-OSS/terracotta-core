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
import com.tctest.spring.bean.FooService;

/**
 * Verifies that BeanNameAware works when bean name is stored in a distributed field
 */
public class ParentChildBean_Test extends SimpleTransparentTestBase {

  public ParentChildBean_Test() {
    disableAllUntil("2008-01-01");
  }

  protected int getNodeCount() {
    return 1;
  }

  protected Class getApplicationClass() {
    return SingletonApp.class;
  }

  public static class SingletonApp extends AbstractSimpleTransparentApp {
    public SingletonApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void doIt() {
      ClassPathXmlApplicationContext ctx1 = new ClassPathXmlApplicationContext(
          "com/tctest/spring/beanfactory-parent-child.xml");

      FooService singleton1 = (FooService) ctx1.getBean("service");

      assertDistributed(ctx1, singleton1);
      assertEquals("rawValue", singleton1.serviceMethod());

      ctx1.close();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

      config.addIncludePattern("com.tctest.spring.bean.SimpleListener", true, true, false);
      config.addIncludePattern("com.tctest.spring.bean.FooServiceImpl", true, true, false);

      {
        DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
        springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
        springConfig.addConfigPattern("*/beanfactory-parent-child.xml");
        springConfig.addBean("service");

        config.addDSOSpringConfig(springConfig);
      }
      
    }

  }
}
