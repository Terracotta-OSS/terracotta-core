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
import com.tctest.spring.bean.MasterBean;
import com.tctest.spring.bean.Singleton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Test for cross context initialization
 */
public class MultipleContexts_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;

  public MultipleContexts_Test() {
    disableAllUntil("2008-01-01");
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
    return MultipleContextsApp.class;
  }
  

  public static class MultipleContextsApp extends AbstractTransparentApp {
    private List sharedSingletons = new ArrayList();
    
    
    public MultipleContextsApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
        try {
          // spring classes should be loaded before using any custom bean classes
          ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] {
              "com/tctest/spring/beanfactory-master.xml", 
              "com/tctest/spring/beanfactory.xml" });
          
          MasterBean distributedMasterBean;
          Singleton singleton;
          
          synchronized (sharedSingletons) {
            // MasterBean localMasterBean = (MasterBean) ctx.getBean("localMasterBean");
            distributedMasterBean = (MasterBean) ctx.getBean("distributedMasterBean");
            distributedMasterBean.addValue(getApplicationId());

            sharedSingletons.add(distributedMasterBean);

            singleton = distributedMasterBean.getSingleton();
          }
          
          moveToStageAndWait(1);
          
          synchronized (sharedSingletons) {
            assertEquals("Expected more objects", NODE_COUNT, sharedSingletons.size());
            
            for (Iterator it = sharedSingletons.iterator(); it.hasNext();) {
              MasterBean o = (MasterBean) it.next();
              List values = o.getValues();
              assertEquals("Missing values "+values, NODE_COUNT, values.size());
              assertTrue("Found non-singleton object "+o, o==distributedMasterBean);
              assertTrue("Found non-singleton object2 "+o.getSingleton(), o.getSingleton()==singleton);
            }
          }
          
        } catch (Throwable e) {
          moveToStage(1);
          notifyError(e);
           
        }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addRoot("com.tctest.spring.MultipleContexts_Test$MultipleContextsApp", "sharedSingletons", "sharedSingletons", false);
      config.addAutolock("* com.tctest.spring.MultipleContexts_Test$MultipleContextsApp.run()", ConfigLockLevel.WRITE);

      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-master.xml");
      springConfig.addConfigPattern("*/beanfactory.xml");
      springConfig.addBean("singleton");
      springConfig.addBean("distributedMasterBean");
      config.addDSOSpringConfig(springConfig);
    }
    
  }

}

