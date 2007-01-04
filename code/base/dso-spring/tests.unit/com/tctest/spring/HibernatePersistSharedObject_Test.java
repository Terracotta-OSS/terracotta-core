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
import com.tctest.spring.bean.HibernatePersister;
import com.tctest.spring.bean.PersistentObject;

/**
 * Test for a problem persisting distributed objects
 */
public class HibernatePersistSharedObject_Test extends SimpleTransparentTestBase {

  public HibernatePersistSharedObject_Test() {
    disableAllUntil("2008-01-01");
  }

  protected int getNodeCount() {
    return 2;
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
          "com/tctest/spring/beanfactory-persist-shared-object.xml");

      HibernatePersister singleton1 = (HibernatePersister) ctx1.getBean("persister");

      assertDistributed(ctx1, "persister", singleton1);
      PersistentObject po = singleton1.make();
      singleton1.changeStatus(po.getMessageId());

      ctx1.close();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addIncludePattern("com.tctest.spring.bean.PersistentObject", true, true, false);
      // config.addIncludePattern("com.tctest.spring.bean.PersistentSubobject", true, true, false);
      // config.addIncludePattern("com.tctest.spring.bean.HibernatePersister", true, true, false);

      {
        DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
        springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
        springConfig.addConfigPattern("*/beanfactory-persist-shared-object.xml");
        springConfig.addBean("persister");

        config.addDSOSpringConfig(springConfig);
      }
      
    }

  }
}
