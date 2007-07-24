/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.FooService;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import junit.framework.Test;

/**
 * Runs a couple of tests within the same JVM
 * 
 * @author cer
 */
public class ParentChildBeanTest extends AbstractTwoServerDeploymentTest {
  private static final String REMOTE_SERVICE_NAME           = "Foo";
  private static final String REMOTE_SERVICE_NAME2          = "Foo2";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-parent-child.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/parent-child-tc-config.xml";

  private FooService   foo1a;
  private FooService   foo2a;
  private FooService   foo1b;
  private FooService   foo2b;

  protected void setUp() throws Exception {
    super.setUp();

    foo1a = (FooService) server0.getProxy(FooService.class, REMOTE_SERVICE_NAME);
    foo2a = (FooService) server1.getProxy(FooService.class, REMOTE_SERVICE_NAME);
    foo1b = (FooService) server0.getProxy(FooService.class, REMOTE_SERVICE_NAME2);
    foo2b = (FooService) server1.getProxy(FooService.class, REMOTE_SERVICE_NAME2);
  }
  
  public void testParentChildBeanDefinitionWithConcreteParent() throws Exception {
    assertEquals("rawValue-0", foo1a.serviceMethod());
    assertEquals("rawValue-1", foo2a.serviceMethod());
  }

  public void testParentChildBeanDefinitionWithConcreteChild() throws Exception {
    assertEquals("another-raw-0", foo1b.serviceMethod());
    assertEquals("another-raw-1", foo2b.serviceMethod());
  }

  private static class ParentChildBeanTestSetup extends SpringTwoServerTestSetup {

    private ParentChildBeanTestSetup() {
      super(ParentChildBeanTest.class, CONFIG_FILE_FOR_TEST, "test-parent-child");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "service1", FooService.class);
      builder.addRemoteService(REMOTE_SERVICE_NAME2, "service2", FooService.class);
    }
  }

  public static Test suite() {
    return new ParentChildBeanTestSetup();
  }

}
