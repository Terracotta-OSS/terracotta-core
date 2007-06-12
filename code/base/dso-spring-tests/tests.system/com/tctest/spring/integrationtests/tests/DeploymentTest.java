/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.Server;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.spring.bean.FooService;
import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.SpringDeploymentTest;

/**
 * Verify various issues with deploying applications
 */
public class DeploymentTest extends SpringDeploymentTest {

  private static final String FOO_SERVICE_NAME       = "Foo";
  private static final String SINGLETON_SERVICE_NAME = "Singleton";
  private Deployment          singletonWAR;
  private Deployment          fooServiceWAR;

  protected void setUp() throws Exception {
    super.setUp();
    singletonWAR = makeSingletonWAR();
    fooServiceWAR = makeFooServiceWAR();
    this.getServerManager().restartDSO(this.isWithPersistentStore());
  }

  protected void runTest() throws Throwable {
    System.err.println("Running test method: " + this.getName());
    super.runTest();
  }

  private Deployment makeSingletonWAR() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder("test-singleton.war");
    builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory.xml");
    builder.addRemoteService(SINGLETON_SERVICE_NAME, "singleton", ISingleton.class);
    builder.addDirectoryOrJARContainingClass(ISingleton.class);
    builder.addDirectoryContainingResource("/tc-config-files/singleton-tc-config.xml");
    return builder.makeDeployment();
  }

  private Deployment makeFooServiceWAR() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder("test-parent-child.war");
    builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory-parent-child.xml");
    builder.addRemoteService(FOO_SERVICE_NAME, "service1", FooService.class);
    builder.addDirectoryOrJARContainingClass(ISingleton.class);
    builder.addDirectoryContainingResource("/tc-config-files/singleton-tc-config.xml");
    return builder.makeDeployment();
  }

  public void testStartAndStopOneWebAppAndThenStartAnother() throws Exception {
    startPingAndStop("test-singleton", singletonWAR, "/tc-config-files/singleton-tc-config.xml",
                     new TestServerCallback() {
                       public void test(Server server) throws Exception {
                         ISingleton singleton1 = (ISingleton) server.getProxy(ISingleton.class, SINGLETON_SERVICE_NAME);
                         singleton1.incrementCounter();

                       }
                     });

    startPingAndStop("test-parent-child", fooServiceWAR, "/tc-config-files/parent-child-tc-config.xml",
                     new TestServerCallback() {
                       public void test(Server server) throws Exception {
                         FooService foo = (FooService) server.getProxy(FooService.class, FOO_SERVICE_NAME);
                         foo.serviceMethod();
                       }
                     });
  }

  private void startPingAndStop(String context, Deployment warFile, String tcConfigPath, TestServerCallback testCallback)
      throws Exception {
    WebApplicationServer server = makeWebApplicationServer(tcConfigPath);
    server.addWarDeployment(warFile, context);
    server.start();

    testCallback.test(server);

    server.stopIgnoringExceptions();
  }

  public void testDeployingTwoWARsOneDistributedOneNot() throws Exception {
    Server server1 = makeServerWithTwoWARsOneDistributed();
    Server server2 = makeServerWithTwoWARsOneDistributed();

    server1.start();
    server2.start();

    SingletonStateUtil.assertSingletonShared(server1, server2, SINGLETON_SERVICE_NAME);

    assertFooServiceTransient(server1, server2);
  }

  private void assertFooServiceTransient(Server server1, Server server2) throws Exception {
    FooService foo1a = (FooService) server1.getProxy(FooService.class, FOO_SERVICE_NAME);
    FooService foo2a = (FooService) server2.getProxy(FooService.class, FOO_SERVICE_NAME);
    assertEquals("rawValue-0", foo1a.serviceMethod());
    assertEquals("rawValue-0", foo2a.serviceMethod());
  }

  private Server makeServerWithTwoWARsOneDistributed() throws Exception {
    return makeWebApplicationServer("/tc-config-files/singleton-tc-config.xml").addWarDeployment(singletonWAR,
                                                                                                 "test-singleton")
        .addWarDeployment(fooServiceWAR, "test-parent-child");
  }

  public void testDeployingTwoDistributedWARs() throws Exception {
    Server server1 = makeServerWithTwoDistributedWARs();
    Server server2 = makeServerWithTwoDistributedWARs();

    server1.start();
    server2.start();

    SingletonStateUtil.assertSingletonShared(server1, server2, SINGLETON_SERVICE_NAME);

    assertFooServiceShared(server1, server2);
  }

  private void assertFooServiceShared(Server server1, Server server2) throws Exception {
    FooService foo1a = (FooService) server1.getProxy(FooService.class, FOO_SERVICE_NAME);
    FooService foo2a = (FooService) server2.getProxy(FooService.class, FOO_SERVICE_NAME);
    assertEquals("rawValue-0", foo1a.serviceMethod());
    assertEquals("rawValue-1", foo2a.serviceMethod());
  }

  private Server makeServerWithTwoDistributedWARs() throws Exception {
    return makeWebApplicationServer("/tc-config-files/singleton-parent-child-tc-config.xml")
        .addWarDeployment(singletonWAR, "test-singleton").addWarDeployment(fooServiceWAR, "test-parent-child");
  }

}
