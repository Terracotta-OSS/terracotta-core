/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Runs a couple of tests within the same JVM
 * 
 * @author cer
 */
public class BeanWithSuperclassTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "Singleton";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-subclass.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/singleton-tc-config.xml";

  private ISingleton singleton1;
  private ISingleton singleton2;

  protected void setUp() throws Exception {
    super.setUp();
    
    singleton1 = (ISingleton) server1.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    singleton2 = (ISingleton) server2.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
  }
  
  public void testSharedField() throws Exception {
    logger.debug("testing shared fields");

    assertEquals(singleton1.getCounter(), singleton2.getCounter());
    singleton1.incrementCounter();
    assertEquals(singleton1.getCounter(), singleton2.getCounter());
    singleton2.incrementCounter();
    assertEquals(singleton2.getCounter(), singleton1.getCounter());

    logger.debug("!!!! Asserts passed !!!");
  }

  public void testTransientField() throws Exception {
    logger.debug("Testing transient fields");
    assertEquals("aaa", singleton1.getTransientValue());
    assertEquals("aaa", singleton2.getTransientValue());
    singleton1.setTransientValue("s1");
    assertEquals("aaa", singleton2.getTransientValue());
    singleton2.setTransientValue("s2");
    assertEquals("s1", singleton1.getTransientValue());
    assertEquals("s2", singleton2.getTransientValue());
    logger.debug("done testing transient fields");
  }
  
  public void testSharedBooleanField() throws Exception {
    assertTrue(singleton1.toggleBoolean());
    assertFalse(singleton2.toggleBoolean());
    assertTrue(singleton1.toggleBoolean());
  }

  private static class SingletonTestSetup extends SpringTwoServerTestSetup {
    private SingletonTestSetup() {
      super(BeanWithSuperclassTest.class, CONFIG_FILE_FOR_TEST, "test-singleton");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class);
    }

  }

  /**
   *  JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new SingletonTestSetup();
    return setup;
  }

}
