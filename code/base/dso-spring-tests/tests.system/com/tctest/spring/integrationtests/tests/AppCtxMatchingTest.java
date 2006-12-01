/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;

import junit.framework.Test;

/**
 * Runs a couple of tests within the same JVM
 */
public class AppCtxMatchingTest extends AbstractTwoServerDeploymentTest {
  private static final String REMOTE_SERVICE_NAME = "Singleton";
  private static final String CONFIG_FILE_FOR_TEST = "/tc-config-files/app-ctx-matching-tc-config.xml";

  static ISingleton   singleton1;
  static ISingleton   singleton2;

  public void testThatSingletonBeanIsNotDistributed() throws Exception {
    assertEquals(0, singleton1.getCounter());
    assertEquals(0, singleton2.getCounter());
    
    singleton1.incrementCounter();
    assertEquals(1, singleton1.getCounter());
    assertEquals(1, singleton2.getCounter());
    
    singleton2.incrementCounter();
    assertEquals(2, singleton1.getCounter());
    assertEquals(2, singleton2.getCounter());
  }

  private static class SingletonTestSetup extends TwoSvrSetup {
    SingletonTestSetup() {
      super(AppCtxMatchingTest.class, CONFIG_FILE_FOR_TEST, "test-singleton");
    }

    protected void setUp() throws Exception {
      super.setUp();

      singleton1 = (ISingleton) server1.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
      singleton2 = (ISingleton) server2.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory.xml");
      // builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory-proxiedbean.xml");
      builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class);
    }

  }

  /**
   *  JUnit test loader entry point
   */
  public static Test suite() {
    return new SingletonTestSetup();
  }

}
