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
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.Singleton;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Test case for Spring JMX support (require Java 5 to run)
 */
public class JmxSupport_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 4;

  public JmxSupport_Test() {
    disableAllUntil("2008-01-01");  //covered by system test
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig()
        .setClientCount(NODE_COUNT)
        .setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return JmxSupportApp.class;
  }
  

  public static class JmxSupportApp extends AbstractTransparentApp {
    
    public JmxSupportApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
        try {
          ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory-jmx.xml");
          
          Singleton singleton = (Singleton) ctx.getBean("singleton");
          singleton.incrementCounter();

          moveToStageAndWait(1);
          
          MBeanServer beanServer = (MBeanServer) ctx.getBean("mbeanServer");
          
          moveToStageAndWait(2);

          Integer counter = (Integer) beanServer.getAttribute(new ObjectName("bean:name=singleton"), "Counter");

          assertEquals("Expecting multiple increments in singleton", NODE_COUNT, singleton.getCounter());
          assertEquals("Expecting multiple increments in mbean", NODE_COUNT, counter.intValue());
          
        } catch (Throwable e) {
          notifyError(e);
          
        }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-jmx.xml");
      springConfig.addBean("singleton");
      config.addDSOSpringConfig(springConfig);
    }
    
  }

}

