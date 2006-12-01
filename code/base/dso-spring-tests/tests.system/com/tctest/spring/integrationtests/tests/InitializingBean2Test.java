/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.bean.ISimpleInitializingSingleton;
import com.tctest.spring.bean.SimpleInitializingSingleton;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Testing the following behavior
 * 1. afterPropertiesSet is invoked properly on the clustered bean instance
 * 2. afterPropertiesSet is invoked on the clustered bean in node 2, instead of the locally created bean 
 * 
 * @author Liyu Yi
 */
public class InitializingBean2Test extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "Singleton";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-init2.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/init2-tc-config.xml";

  private static ISimpleInitializingSingleton   singleton1;
  private static ISimpleInitializingSingleton   singleton2;

  public void testInitialization() throws Exception {

    logger.debug("testing initialization");
    
    // test pre-condition
    assertEquals("Pre-condition check failed.", SimpleInitializingSingleton.ME, singleton1.getName());    // instantiate bean in node1
    assertEquals("Pre-condition check failed.", SimpleInitializingSingleton.ME, singleton1.getName());    // instantiate bean in node1
    
    long id1 = singleton1.getId();
    long id2 = singleton2.getId();
    long innerId1 = singleton1.getInnerId();
    long innerId2 = singleton2.getInnerId();
    
    // check initialization in node 1
    assertEquals(id1, innerId1);
    assertTrue(singleton1.isTheSameInstance());
    
    // check initialization in node 2
    // node 2 will get shared singlton from node 1
    // but pupulated with transient local values
    // including "id" and local "afterPropertiesSetThis"
    assertEquals(id2, innerId2);
    assertTrue(id1!=id2);
    // this is still true since afterPropertiesSet is passed in with the clustered object
    assertTrue("Failed to verify that afterPropertiesSet is invoked on the shared bean", singleton2.isTheSameInstance());
    
    logger.debug("!!!! Asserts passed !!!");
  }

  private static class InitializingBean2Setup extends TwoSvrSetup {
    private InitializingBean2Setup() {
      super(InitializingBean2Test.class, CONFIG_FILE_FOR_TEST, "test-initializingbean2");
    }

    protected void setUp() throws Exception {
      super.setUp();

      singleton1 = (ISimpleInitializingSingleton) server1.getProxy(ISimpleInitializingSingleton.class, REMOTE_SERVICE_NAME);
      singleton2 = (ISimpleInitializingSingleton) server2.getProxy(ISimpleInitializingSingleton.class, REMOTE_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "distributedInitBeanProxy", ISimpleInitializingSingleton.class);
    }

  }

  /**
   *  JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new InitializingBean2Setup();
    return setup;
  }

}
