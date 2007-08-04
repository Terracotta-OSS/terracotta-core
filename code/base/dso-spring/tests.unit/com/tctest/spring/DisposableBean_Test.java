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
import com.tctest.spring.bean.DisposableService;

/**
 * Spring singleton test
 */
public class DisposableBean_Test extends SimpleTransparentTestBase {

  public DisposableBean_Test() {
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
                             
          "com/tctest/spring/beanfactory-disposable.xml");

      DisposableService singleton1 = (DisposableService) ctx1.getBean("disposable");
      
      assertDistributed(ctx1, "disposable", singleton1);
      
      assertSame(singleton1, DisposableService.afterPropertiesSetThis);

      assertEquals("disposable", singleton1.getName());

      assertNotNull(singleton1.getFoo());
      
      ctx1.close();

//      assertNull(singleton1.getName());
      assertSame(singleton1, DisposableService.destroyThis);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-disposable.xml");
      springConfig.addBean("disposable");

      config.addDSOSpringConfig(springConfig);

      config.addIncludePattern("com.tctest.spring.bean.SimpleListener", true, true, false);
      config.addIncludePattern("com.tctest.spring.bean.DisposableService", true, true, false);

    }

  }
}
