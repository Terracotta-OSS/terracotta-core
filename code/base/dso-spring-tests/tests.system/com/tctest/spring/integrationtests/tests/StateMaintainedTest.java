/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.spring.bean.ISingleton;

public class StateMaintainedTest extends AbstractDeploymentTest {

  private static final String APP_NAME = "test-singleton";
  private static final String REMOTE_SERVICE_NAME           = "Singleton";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/singleton-tc-config.xml";

  private WebApplicationServer server1;
  private WebApplicationServer server2;

  
  protected void setUp() throws Exception {
    super.setUp();
    Deployment deployment = makeDeployment();

    server1 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST);
    server1.addWarDeployment(deployment, APP_NAME);
    
    server2 = makeWebApplicationServer(CONFIG_FILE_FOR_TEST);
    server2.addWarDeployment(deployment, APP_NAME);
  }

  public void testThatStatePreservedAcrossServerRestart() throws Exception {
    startServer1();
    
    server1.restart();
    
    ISingleton singleton1 = (ISingleton) server1.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    assertEquals(1, singleton1.getCounter());
  }

  private void startServer1() throws Exception {
    server1.start();
    ISingleton singleton1 = (ISingleton) server1.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    assertEquals(0, singleton1.getCounter());
    singleton1.incrementCounter();
    assertEquals(1, singleton1.getCounter());
  }
  
  public void testThatNewlyStartedServerGetsDistributedState() throws Exception {
    
    startServer1();
    
    server2.start();
    ISingleton singleton2 = (ISingleton) server2.getProxy(ISingleton.class, REMOTE_SERVICE_NAME);
    assertEquals(1, singleton2.getCounter());
  }
  
  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(APP_NAME + ".war");
    addBeanDefinitions(builder);
    configureRemoteInterfaces(builder);
    addClassesAndLibraries(builder);
    return builder.makeDeployment();
  }

  private void addBeanDefinitions(DeploymentBuilder builder) {
    builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
  }

  private void configureRemoteInterfaces(DeploymentBuilder builder) {
    builder.addRemoteService(REMOTE_SERVICE_NAME, "singleton", ISingleton.class);
  }

  private void addClassesAndLibraries(DeploymentBuilder builder) {
    builder.addDirectoryOrJARContainingClass(ISingleton.class);
    builder.addDirectoryContainingResource(CONFIG_FILE_FOR_TEST);
  }

}
