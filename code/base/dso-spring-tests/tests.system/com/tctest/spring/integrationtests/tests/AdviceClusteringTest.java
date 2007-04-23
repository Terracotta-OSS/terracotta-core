/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.CounterSaver;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.bean.ISingletonAdvice;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Testing the following features
 * 1. Introduction and pointcut advices retain the original semantics
 * 2. Introduction and pointcut advices are also shared
 * 
 * @author Liyu Yi
 */
/**
 * Testing Singleton works and also 
 */
public class AdviceClusteringTest extends AbstractTwoServerDeploymentTest {

  private static final String PC_SERVICE_NAME           = "SingletonAdvice";
  private static final String IN_SERVICE_NAME           = "CounterSaver";
  private static final String SINGLETON_SERVICE_NAME    = "Singlton";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-aop.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/aop-tc-config.xml";

  private static ISingleton   singletonWithPC1;
  private static ISingleton   singletonWithPC2;
  private static CounterSaver counterSaver1;
  private static CounterSaver counterSaver2;
  private static ISingletonAdvice pcAdvice1;
  private static ISingletonAdvice pcAdvice2;

  public void testAdviceClustering() throws Exception {
    logger.debug("testing shared aspects");
    
    int singletonCnt1 = singletonWithPC1.getCounter();  // advice counter increased
    int singletonCnt2 = singletonWithPC2.getCounter();  // advice counter increased
    
    assertEquals("Pre-condition not met for singletonCnt1", 0, singletonCnt1);
    assertEquals("Pre-condition not met for singletonCnt2", 0, singletonCnt2);
    assertEquals("Pre-condition not met for counterSaver1", 0, counterSaver1.getSavedCounter());
    assertEquals("Pre-condition not met for counterSaver2", 0, counterSaver2.getSavedCounter());

    // check pointcut advice sharing
    int adviceCnt1 = pcAdvice1.getCounter();
    int adviceCnt2 = pcAdvice2.getCounter();
    assertEquals("PointCut advice is not working for pcAdvice1", 2, adviceCnt1);
    assertEquals("PointCut advice is not working for pcAdvice2", 2, adviceCnt2);

    // check bean sharing
    singletonWithPC1.incrementCounter();  // is shared
    assertEquals("Shared bean is not working for singletonWithPC2", 1, singletonWithPC2.getCounter());
    
    // check introduction sharing
    counterSaver2.saveCounter();  // saved counter is shared; both saved counter should be set
    assertEquals("Shared introduction is not working for singletonWithPC2", 1, counterSaver2.getSavedCounter());
    
    logger.debug("!!!! Asserts passed !!!");
  }

  private static class AdviceClusteringTestSetup extends TwoSvrSetup {
    private AdviceClusteringTestSetup() {
      super(AdviceClusteringTest.class, CONFIG_FILE_FOR_TEST, "test-adviceclustering");
    }

    protected void setUp() throws Exception {
      super.setUp();
      singletonWithPC1 = (ISingleton) server1.getProxy(ISingleton.class, SINGLETON_SERVICE_NAME);
      singletonWithPC2 = (ISingleton) server2.getProxy(ISingleton.class, SINGLETON_SERVICE_NAME);
      counterSaver1 = (CounterSaver) server1.getProxy(CounterSaver.class, IN_SERVICE_NAME);
      counterSaver2 = (CounterSaver) server2.getProxy(CounterSaver.class, IN_SERVICE_NAME);
      pcAdvice1 = (ISingletonAdvice) server1.getProxy(ISingletonAdvice.class, PC_SERVICE_NAME);
      pcAdvice2 = (ISingletonAdvice) server2.getProxy(ISingletonAdvice.class, PC_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(IN_SERVICE_NAME, "singletonWithCounterSaver", CounterSaver.class);
      builder.addRemoteService(PC_SERVICE_NAME, "singletonAdvice", ISingletonAdvice.class);
      builder.addRemoteService(SINGLETON_SERVICE_NAME, "singletonWithGetCounter", ISingleton.class);
     }
  }

  /**
   *  JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new AdviceClusteringTestSetup();
    return setup;
  }
  
}
