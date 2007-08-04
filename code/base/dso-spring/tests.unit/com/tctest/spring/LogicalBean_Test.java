/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;

import java.util.List;

/**
 * Test for logically managed beans, such as java.util.ArrayList
 */
public class LogicalBean_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;

  public LogicalBean_Test() {
     //
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LogicalBeanApp.class;
  }
  

  public static class LogicalBeanApp extends AbstractTransparentApp {
    
    public LogicalBeanApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      try {
        moveToStageAndWait(10);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/logicalbeans.xml");
        
        List list = (List) ctx.getBean("logicalBean");
        synchronized (list) {
          list.add(getApplicationId());
        }
        
        moveToStageAndWait(11);

        synchronized (list) {
          assertEquals("Expected more then one object in the collection", NODE_COUNT, list.size());
        }

        moveToStageAndWait(12);
        
      } catch (Throwable e) {
        moveToStage(11);
        moveToStage(12);
        notifyError(e);
        
      }
    }
    
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addIncludePattern("com.tctest.spring.LogicalBean_Test$LogicalBeanApp");
      config.addAutolock("* com.tctest.spring.LogicalBean_Test$LogicalBeanApp.run()", ConfigLockLevel.WRITE);
      
      {
        DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
        springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
        springConfig.addConfigPattern("*/logicalbeans.xml");
        springConfig.addBean("logicalBean");
        
        config.addDSOSpringConfig(springConfig);
      }
    }
    
  }

}

