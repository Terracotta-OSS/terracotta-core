/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

/**
 * Spring singleton test
 */
public class InvalidBeanDefinition_Test extends SimpleTransparentTestBase {

  public InvalidBeanDefinition_Test() {
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

      try {
        ClassPathXmlApplicationContext ctx1 = new ClassPathXmlApplicationContext(
            "com/tctest/spring/beanfactory-interceptor-with-error.xml");
        assertDistributed(ctx1, "service", ctx1.getBean("service"));
        fail("expected BeanCreationException");
      } catch (BeanCreationException e) {
        // expected
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-interceptor-with-error.xml");
      springConfig.addBean("service");

      config.addDSOSpringConfig(springConfig);

      config.addIncludePattern("com.tctest.spring.bean.SimpleListener", true, true, false);
      config.addIncludePattern("com.tctest.spring.bean.BarServiceImpl", true, true, false);
      config.addIncludePattern("com.tctest.spring.bean.FooServiceImpl", true, true, false);

    }

  }
}
