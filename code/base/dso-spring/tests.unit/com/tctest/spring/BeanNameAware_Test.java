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
import com.tctest.spring.bean.BeanNameAwareBean;

/**
 * Spring singleton test
 */
public class BeanNameAware_Test extends SimpleTransparentTestBase {

  public BeanNameAware_Test() {
    disableAllUntil("2008-01-01");  //covered by system test
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

      "com/tctest/spring/beanfactory-bean-name-aware.xml");

      BeanNameAwareBean singleton1 = (BeanNameAwareBean) ctx1.getBean("aware");

      assertDistributed(ctx1, "aware", singleton1);

      assertEquals("aware", singleton1.getName());

      ctx1.close();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-bean-name-aware.xml");
      springConfig.addBean("aware");

      config.addDSOSpringConfig(springConfig);

      config.addIncludePattern("com.tctest.spring.bean.SimpleListener", true, true, false);
      config.addIncludePattern("com.tctest.spring.bean.BeanNameAwareBean", true, true, false);

    }

  }
}
