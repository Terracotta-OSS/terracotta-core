/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;

/**
 * Tests for Spring error reporting 
 */
public class ErrorReporting_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 1;

  public ErrorReporting_Test() {
    // disableAllUntil("2008-01-01");
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
    return ErrorReportingApp.class;
  }
  

  public static class ErrorReportingApp extends AbstractTransparentApp {
    
    public ErrorReportingApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      testErrorReporting();
    }

    private void testErrorReporting() {
      try {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/ErrorReporting.xml");
        ctx.getBean("bean1");
        fail("Epected to see Spring exception");
      } catch (CannotLoadBeanClassException e) {
        // expected
      } catch (BeanDefinitionStoreException e) {
        // ignore
      }      
    }
    
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
      springConfig.addConfigPattern("*/ErrorReporting.xml");
      springConfig.addBean("bean1");
      springConfig.addBean("bean2");      
      config.addDSOSpringConfig(springConfig);
    }
    
  }

}

