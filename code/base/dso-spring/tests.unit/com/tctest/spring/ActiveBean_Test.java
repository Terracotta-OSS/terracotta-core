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
import com.tctest.spring.bean.ActiveBean;

import java.util.List;

/**
 * ActiveBean test
 */
public class ActiveBean_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;

  public ActiveBean_Test() {
    disableAllUntil("2008-01-01");
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return ActiveBeanApp.class;
  }
  

  public static class ActiveBeanApp extends AbstractTransparentApp {
    
    public ActiveBeanApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
        testActiveBean();
    }

    private void testActiveBean() {
      try {
        moveToStageAndWait(10);

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory-active.xml");
        
        ActiveBean activeBean = (ActiveBean) ctx.getBean("activeBean");

        moveToStageAndWait(11);

        Thread.sleep(500L);  // to make sure that thread will start
        
        List instances = activeBean.getInstances();
        assertEquals("Expecting single bean instance "+instances, 1, instances.size());
        
        moveToStageAndWait(12);
          
        ctx.close();

        moveToStageAndWait(13);

        instances = activeBean.getInstances();
        
        assertTrue("Active bean is not stopped", activeBean.isStopped());
        
        moveToStageAndWait(14);

        assertEquals("Expecting no instances "+instances, 0, instances.size());
        
      } catch (Throwable e) {
        moveToStage(10);
        moveToStage(11);
        moveToStage(12);
        moveToStage(13);
        moveToStage(14);
        notifyError(e);
        
      } finally {
        //  
      }
    }
    
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-active.xml");
      springConfig.addBean("activeBean");
      
      config.addDSOSpringConfig(springConfig);
    }
    
  }

}

