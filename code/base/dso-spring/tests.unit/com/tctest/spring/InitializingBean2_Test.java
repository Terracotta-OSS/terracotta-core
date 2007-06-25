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
import com.tctest.spring.bean.SimpleInitializingSingleton;

/**
 * Test for <code>InitializingBean</code>.
 * 
 * @see org.springframework.beans.factory.InitializingBean
 */
public class InitializingBean2_Test extends SimpleTransparentTestBase {

  public InitializingBean2_Test() {
    //
  }

  protected int getNodeCount() {
    return 1;
  }

  
  protected Class getApplicationClass() {
    return InitializingBeanApp.class;
  }

  public static class InitializingBeanApp extends AbstractSimpleTransparentApp {

    public InitializingBeanApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    protected void doIt() {
      ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory-init2.xml");

      SimpleInitializingSingleton singleton = (SimpleInitializingSingleton) ctx.getBean("distributedInitBean");

      assertDistributed(ctx, "distributedInitBean", singleton);
      
      assertSame(singleton, SimpleInitializingSingleton.afterPropertiesSetThis);
      
      assertNotNull(singleton.getName());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME); // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-init2.xml");
      springConfig.addBean("recorder");
      springConfig.addBean("distributedInitBean");
      config.addDSOSpringConfig(springConfig);

      config.addIncludePattern("com.tctest.spring.bean.SimpleInitializingSingleton", true, true, false);

    }

  }

}
