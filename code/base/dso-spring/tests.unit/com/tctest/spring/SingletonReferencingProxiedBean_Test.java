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
 * Test attempted to reproduce JIRA issue LKC-1435 where proxies "disappear" when
 * distributed bean references a proxied non-distributed bean.
 */
public class SingletonReferencingProxiedBean_Test extends SimpleTransparentTestBase {

  public SingletonReferencingProxiedBean_Test() {
    disableAllUntil("2008-01-01");  //covered by system test
  }

  protected int getNodeCount() {
    return 3;
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
          "com/tctest/spring/beanfactory-interceptor2.xml");

      FooService singleton1 = (FooService) ctx1.getBean("service");

      assertDistributed(ctx1, "service", singleton1);

      assertEquals("barAndinterceptorInvoked", singleton1.serviceMethod());
    }


    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-interceptor2.xml");
      springConfig.addBean("service");
      springConfig.excludeField("service", "serviceHelper");

      config.addDSOSpringConfig(springConfig);

      config.addIncludePattern("com.tctest.spring.bean.SimpleListener", true, true, false);
      config.addIncludePattern("com.tctest.spring.bean.BarServiceImpl", true, true, false);
    }

  }
}
