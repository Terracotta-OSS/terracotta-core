/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.ApplicationContext;
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
import com.tctest.spring.bean.CounterSaver;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.bean.SingletonAdvice;

/**
 * Test case for Spring Proxy-based AOP framework 
 */
public class AdviceClustering_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;
  
  public AdviceClustering_Test() {
    disableAllUntil("2008-01-01");  //covered by system test
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return AdviceClusteringApp.class;
  }
  

  public static class AdviceClusteringApp extends AbstractTransparentApp {
    
    public AdviceClusteringApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
      ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory-aop.xml");
      testAroundAdvice(ctx);
      testIntroductionAdvice(ctx);
    }

    private void testAroundAdvice(ClassPathXmlApplicationContext ctx) {
      try {
        ISingleton singleton = (ISingleton) ctx.getBean("singletonWithGetCounter");
        
        moveToStageAndWait(10);
  
        singleton.getCounter();
  
        moveToStageAndWait(11);
        
        SingletonAdvice advice = (SingletonAdvice) ctx.getBean("singletonAdvice");
        int counter = advice.getCounter();
        assertEquals("Wrong number of invocations", NODE_COUNT, counter);
      
      } catch (Throwable e) {
        moveToStage(10);
        moveToStage(11);
        notifyError(e);
      }
    }

    private void testIntroductionAdvice(ApplicationContext ctx) {
      try {
        Object o = ctx.getBean("singletonWithCounterSaver");
        ISingleton singleton = (ISingleton) o;
        CounterSaver saver = (CounterSaver) singleton;
        
        moveToStageAndWait(20);
        
        int savedCounter;
  
        synchronized(singleton) {
  
          if(saver.getSavedCounter()==0) {
            singleton.incrementCounter();
            singleton.incrementCounter();
          
            saver.saveCounter();
          }
  
          savedCounter = singleton.getCounter();
        }
        
        moveToStageAndWait(21);
  
        singleton.incrementCounter();
        singleton.incrementCounter();
        
        moveToStageAndWait(22);
        
        assertEquals("Wrong saved counter", savedCounter, saver.getSavedCounter());
      
      } catch (Throwable e) {
        moveToStage(20);
        moveToStage(21);
        notifyError(e);
      }
    }
    
    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      // config.addAutolock("* com.tctest.spring.Singleton_Test$AdviceClusteringApp.run()", ConfigLockLevel.WRITE);

      // config.addIncludePattern("com.tctest.spring.bean.Singleton", true, true);
      // config.addIncludePattern("com.tctest.spring.bean.SingletonAdvice", true, true);
      // config.addIncludePattern("com.tctest.spring.bean.CounterSaverMixinAdvisor", true, true);
      config.addIncludePattern("com.tctest.spring.bean.CounterSaverMixin", true, true, false);
      config.addAutolock("* com.tctest.spring.bean.CounterSaverMixin.*(..)", ConfigLockLevel.WRITE);
      
      // config.addAutolock("* com.tctest.spring.bean.Singleton.incrementCounter()", ConfigLockLevel.WRITE);
      // config.addAutolock("* com.tctest.spring.bean.SingletonAdvice.invoke(..)", ConfigLockLevel.WRITE);
      // config.addAutolock("* com.tctest.spring.bean.CounterSaverMixin.invoke(..)", ConfigLockLevel.WRITE);
      
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.setFastProxyEnabled(true);
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory-aop.xml");
      springConfig.addBean("singletonImpl1");
      springConfig.addBean("singletonImpl2");
      springConfig.addBean("singletonAdvice");
      springConfig.addBean("singletonCounterSaverAdvisor");
      
      config.addDSOSpringConfig(springConfig);
    }
    
  }

}

