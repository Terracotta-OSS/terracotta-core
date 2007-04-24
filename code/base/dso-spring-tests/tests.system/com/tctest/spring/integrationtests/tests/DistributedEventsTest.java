/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.TestCallback;
import com.tctest.spring.bean.EventManager;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import junit.framework.Test;


public class DistributedEventsTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "EventManager";

  private EventManager eventManager1;
  private EventManager eventManager2;

//  public DistributedEventsTest() {
//    disableAllUntil("2007-02-27");
//  }

  protected void setUp() throws Exception {
    super.setUp();
    
    eventManager1 = (EventManager) server1.getProxy(EventManager.class, REMOTE_SERVICE_NAME);
    eventManager2 = (EventManager) server2.getProxy(EventManager.class, REMOTE_SERVICE_NAME);

    eventManager1.clear();
    eventManager2.clear();
    
    assertEquals(0, eventManager1.size());
    assertEquals(0, eventManager2.size());
  }

  public void testDistributedEvent() throws Throwable {
    eventManager1.publishEvents("foo1", "bar1", 3);
    eventManager1.publishEvents("foo1", "bar2", 4);

    waitForSuccess(60 * 3, new TestCallback() {
      public void check() {
        assertEquals("Got local", 7, eventManager1.size());
        assertEquals("Should be distributed", 7, eventManager2.size());
      }
    });
  }
  
  public void testNonDistributedEvent() throws Throwable {
    eventManager1.publishLocalEvent("foo2", "bar1");
    Thread.sleep(1000L * 5);
    assertEquals("Got local", 1, eventManager1.size());
    assertEquals("Should not be distributed", 0, eventManager2.size());
  }
  
  private static class SingletonTestSetup extends SpringTwoServerTestSetup {

    private SingletonTestSetup() {
      super(DistributedEventsTest.class, "/tc-config-files/event-tc-config.xml", "distributed-events");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile("classpath:/com/tctest/spring/distributedevents.xml");
      builder.addRemoteService(REMOTE_SERVICE_NAME, "eventManager", EventManager.class);
    }

  }

  public static Test suite() {
    return new SingletonTestSetup();
  }
}
