/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Test the clustered bean behavior with multiple contexts; 
 * verify that the clustering behavior is within "same" context - context with the same 
 * 
 * @author Liyu Yi
 */
public class SingletonFromAnotherContextTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "Singleton";
  private static final String ANOTHER_REMOTE_SERVICE_NAME   = "AnotherSingleton";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-another.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/anothersingleton-tc-config.xml";

  private static ISingleton   singleton1N1;
  private static ISingleton   singleton2N1;

  private static ISingleton   singleton1N2;
  private static ISingleton   singleton2N2;

  public void testSingletonFromAnotherContext() throws Exception {

    logger.debug("testing singleton from another context");
    
    // check pre-condition
    assertEquals(0, singleton1N1.getCounter());
    assertEquals(0, singleton1N2.getCounter());
    
    assertEquals(0, singleton2N1.getCounter());
    assertEquals(0, singleton2N2.getCounter());
    
    singleton1N1.incrementCounter();
    singleton1N2.incrementCounter();
    
    // only singleton1s are getting the changes
    assertEquals(2, singleton1N1.getCounter());
    assertEquals(2, singleton1N2.getCounter());
    
    assertEquals(0, singleton2N1.getCounter());
    assertEquals(0, singleton2N2.getCounter());

    singleton2N1.incrementCounter();
    singleton2N2.incrementCounter();
    
    // only singleton2s are getting the changes
    assertEquals(2, singleton2N1.getCounter());
    assertEquals(2, singleton2N2.getCounter());
       
    assertEquals(2, singleton1N1.getCounter());
    assertEquals(2, singleton1N2.getCounter());
    
    logger.debug("!!!! Asserts passed !!!");
  }

  private static class InnerTestSetup extends TwoSvrSetup {
    private InnerTestSetup() {
      super(SingletonFromAnotherContextTest.class, CONFIG_FILE_FOR_TEST, "test-anothersingleton");
    }

    protected void setUp() throws Exception {
      super.setUp();

      singleton1N1 = (ISingleton) server1.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
      singleton2N1 = (ISingleton) server1.getProxy(ISingleton.class, ANOTHER_REMOTE_SERVICE_NAME);

      singleton1N2 = (ISingleton) server2.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
      singleton2N2 = (ISingleton) server2.getProxy(ISingleton.class, ANOTHER_REMOTE_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class);
      builder.addRemoteService(ANOTHER_REMOTE_SERVICE_NAME, "anotherSingleton", ISingleton.class);
    }
  }

  /**
   *  JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new InnerTestSetup();
    return setup;
  }
}
