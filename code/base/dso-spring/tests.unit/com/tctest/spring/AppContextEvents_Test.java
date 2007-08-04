/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.StandardDSOSpringConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.runner.AbstractTransparentApp;
import com.tctest.spring.bean.SimpleListener;
import com.tctest.spring.bean.SingletonEvent;

import java.util.Iterator;
import java.util.List;

/**
 * Test case for <code>ApplicationEventPublisher</code>.
 * 
 * @see org.springframework.context.ApplicationEventPublisher#publishEvent(ApplicationEvent event)
 */
public class AppContextEvents_Test extends TransparentTestBase {
  private static final int LOOP_ITERATIONS = 1;
  private static final int EXECUTION_COUNT = 1;
  private static final int NODE_COUNT      = 2;

  public AppContextEvents_Test() {
    //
  }

  protected void setUp() throws Exception {
    super.setUp();
    getTransparentAppConfig().setClientCount(NODE_COUNT).setApplicationInstancePerClientCount(EXECUTION_COUNT)
        .setIntensity(LOOP_ITERATIONS);
    initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return AppContextEventsApp.class;
  }

  
  public static class AppContextEventsApp extends AbstractTransparentApp {

    public AppContextEventsApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public void run() {
        try {
          ClassPathXmlApplicationContext ctx1 = new ClassPathXmlApplicationContext("com/tctest/spring/beanfactory.xml");
          ClassPathXmlApplicationContext ctx2 = 
            new ClassPathXmlApplicationContext(new String[] {"com/tctest/spring/beanfactory3.xml"}, ctx1);
          
          SimpleListener simpleListener = (SimpleListener) ctx1.getBean("simpleListener");
          List events = simpleListener.getEvents();
          
          moveToStageAndWait(1);

          ctx1.publishEvent(new SingletonEvent("ctx1", "Test event1 "+getApplicationId()));
          moveToStageAndWait(2);

          waitEvents(events, NODE_COUNT, 5000L);
          assertEquals("Haven't received distributed events: "+events, NODE_COUNT, events.size());

          moveToStageAndWait(3);
          
          ctx2.publishEvent(new SingletonEvent("ctx2", "Test event2 "+getApplicationId()));
          moveToStageAndWait(4);

          waitEvents(events, NODE_COUNT * 2, 5000L);
          
          int ctx2Count = 0;
          for (Iterator it = events.iterator(); it.hasNext();) {
            SingletonEvent e = (SingletonEvent) it.next();
            if(e.getSource().equals("ctx2")) ctx2Count++;
          }
          
          assertEquals("Haven't received distributed events: "+events, NODE_COUNT, ctx2Count);
          
        } catch (Throwable e) {
          moveToStage(1);
          moveToStage(2);
          moveToStage(3);
          notifyError(e);
           
        }
    }

    private void waitEvents(List events, int count, long timeout) {
      long time = System.currentTimeMillis();
      while(events.size()<count && (System.currentTimeMillis()-time)<timeout) {
        try {
          Thread.sleep(50L);
        } catch (Exception e) {
          // ignore
        }
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.addIncludePattern("com.tctest.spring.bean.SingletonEvent", true, true, false);
      
      DSOSpringConfigHelper springConfig = new StandardDSOSpringConfigHelper();
      springConfig.addApplicationNamePattern(SpringTestConstants.APPLICATION_NAME);  // app name used by testing framework
      springConfig.addConfigPattern("*/beanfactory.xml");
      springConfig.addConfigPattern("*/beanfactory3.xml");
      springConfig.addDistributedEvent("com.tctest.spring.bean.SingletonEvent");
      config.addDSOSpringConfig(springConfig);
    }
    
  }
  
}
