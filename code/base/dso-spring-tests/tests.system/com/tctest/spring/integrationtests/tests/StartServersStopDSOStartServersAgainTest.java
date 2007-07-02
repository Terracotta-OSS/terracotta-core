/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.Server;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.SpringDeploymentTest;

import junit.framework.Test;


public class StartServersStopDSOStartServersAgainTest extends SpringDeploymentTest {
  private static final String  REMOTE_SERVICE_NAME           = "Singleton";
  private static final String  BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory.xml";
  private static final String  CONFIG_FILE_FOR_TEST          = "/tc-config-files/singleton-tc-config.xml";

  public static Test suite() {
    return new MyTestSetup(StartServersStopDSOStartServersAgainTest.class);
  }
  
  private static class MyTestSetup extends ServerTestSetup {

    public MyTestSetup(Class testClass) {
      super(testClass);
    }
    
    public boolean isWithPersistentStore() {
      return true;
    } 
    
  }
  
  public StartServersStopDSOStartServersAgainTest() {
    //disableAllUntil("2010-03-01");
  }
  
  public void testStartServersStopDSOStartServersAgain() throws Exception {
    
    Deployment warPath = makeDeploymentBuilder("test-singleton.war")
        .addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST)
        .addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class)
        .addDirectoryOrJARContainingClass(ISingleton.class)
        .addDirectoryContainingResource(CONFIG_FILE_FOR_TEST)
        .makeDeployment();

    Server server1 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST).addWarDeployment(warPath, "test-singleton");
    Server server2 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST).addWarDeployment(warPath, "test-singleton");
    server1.start();
    logger.info("*** Started server1");
    server2.start();
    logger.info("*** Started server2");

    ISingleton singleton1 = (ISingleton) server1.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    ISingleton singleton2 = (ISingleton) server2.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);

    assertCounterShared(singleton1, singleton2);
    logger.info("*** Assertion1 passed");

    restartDSO();
    logger.info("*** DSO restarted");

    Server server3 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST).addWarDeployment(warPath, "test-singleton");
    server3.start();
    logger.info("*** Started server3");

    Server server4 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST).addWarDeployment(warPath, "test-singleton");
    server4.start();
    logger.info("*** Started server4");

    ISingleton singleton3 = (ISingleton) server3.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    ISingleton singleton4 = (ISingleton) server4.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);

    assertCounterShared(singleton3, singleton4);
    
    logger.info("*** Test completed");
  }

  private void assertCounterShared(ISingleton singleton1, ISingleton singleton2) {
    assertEquals(singleton1.getCounter(), singleton2.getCounter());
    
    singleton1.incrementCounter();
    assertEquals(singleton1.getCounter(), singleton2.getCounter());
    
    singleton2.incrementCounter();
    assertEquals(singleton2.getCounter(), singleton1.getCounter());
  }
  
  public boolean isWithPersistentStore() {
    return true;
  }

}

