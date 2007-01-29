/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.bean.EventManager;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.TestCallback;

import junit.extensions.TestSetup;
import junit.framework.Test;


public class DistributedEventsTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "EventManager";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/distributedevents.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/event-tc-config.xml";

  private static EventManager eventManager1;
  private static EventManager eventManager2;

  protected void setUp() throws Exception {
    super.setUp();
    eventManager1.clear();
    eventManager2.clear();
    assertEquals(0, eventManager1.size());
    assertEquals(0, eventManager2.size());
  }
  
  private void assertEventNotDistributed() throws InterruptedException {
    Thread.sleep(1000L * 5);
    assertEquals("Got local", 1, eventManager1.size());
    assertEquals("Should not be distributed", 0, eventManager2.size());
  }

  private void assertEventDistributed() throws Throwable {
    waitForSuccess(60 * 3, new TestCallback() {
      public void check() {
        assertEquals("Got local", 1, eventManager1.size());
        assertEquals("Should be distributed", 1, eventManager2.size());
      }
    });
  }

  public void testDistributedEvent() throws Throwable {
    eventManager1.publishEvent("foo", "bar");
    assertEventDistributed();
  }

  public void testNonDistributedEvent() throws Throwable {
    eventManager1.publishLocalEvent("foo", "bar");
    assertEventNotDistributed();
  }

//  Not sure what is tested here. Parent class does not have any fields. So, it should be ok  
//  public void testPublishEventWithUninstrumentedSuperclass() throws Throwable {
//    publishDistributedEvent(new AnotherEventExpectedToBeDistributed("foo", "bar"));
//  }

//  These are covered by the unit test  
//  public void testPublishWildcardAtEnd() throws Throwable {
//    publishDistributedEvent(new WildcardAtEndEvent("foo", "bar"));
//  }
//
//  public void testPublishWildcardAtStart() throws Throwable {
//    publishDistributedEvent(new WildcardAtStartEvent("foo", "bar"));
//  }
//
//  public void testPublishWildcardAtBothEnds() throws Throwable {
//    publishDistributedEvent(new WildcardAtBothEndsEvent("foo", "bar"));
//  }

  
  private static class SingletonTestSetup extends TwoSvrSetup {

    private SingletonTestSetup() {
      super(DistributedEventsTest.class, CONFIG_FILE_FOR_TEST, "distributed-events");
    }

    protected void setUp() throws Exception {
      super.setUp();

      eventManager1 = (EventManager) server1.getProxy(EventManager.class, REMOTE_SERVICE_NAME);
      eventManager2 = (EventManager) server2.getProxy(EventManager.class, REMOTE_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "eventManager", EventManager.class);
    }

  }

  public static Test suite() {
    TestSetup setup = new SingletonTestSetup();
    return setup;
  }
}
