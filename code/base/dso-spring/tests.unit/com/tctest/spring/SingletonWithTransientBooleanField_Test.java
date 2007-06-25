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
import com.tctest.spring.bean.SingletonWithTransientField;

/**
 * Test for JIRA issue LKC-1490  Unfortunately, it does not fail.
 */

public class SingletonWithTransientBooleanField_Test extends SimpleTransparentTestBase {

  public SingletonWithTransientBooleanField_Test() {
    //
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
          "com/tctest/spring/beanfactory-transientField.xml");

      SingletonWithTransientField singleton1 = (SingletonWithTransientField) ctx1.getBean("service");
      assertTrue(singleton1.isTransientValue());
      assertDistributed(ctx1, "service", singleton1);

      moveToStageAndWait(1);
      singleton1.setTransientValue(false);
      moveToStageAndWait(2);

      assertFalse(singleton1.isTransientValue());

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-transientField.xml");
      springConfig.addBean("service");
      springConfig.excludeField("service", "transientValue");
      config.addDSOSpringConfig(springConfig);

      config.addIncludePattern("com.tctest.spring.bean.SimpleListener", true, true, false);
      config.addIncludePattern("com.tctest.spring.bean.SingletonWithTransientField", true, false, false);
      config.addIncludePattern("com.tctest.spring.bean.BaseSingletonWithTransientField", true, false, false);

    }

  }
}
