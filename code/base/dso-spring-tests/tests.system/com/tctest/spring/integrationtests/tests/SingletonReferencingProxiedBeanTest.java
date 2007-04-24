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
 * Testing the following features
 * 1. proxy is correctly invoked
 * 2. The shared bean is cached in the mixin cache
 * 
 * @author Liyu Yi
 */
public class SingletonReferencingProxiedBeanTest extends AbstractTwoServerDeploymentTest {

  private static final String FOO_SERVICE_NAME           = "FooService";
  private static final String BAR_SERVICE_NAME           = "BarService";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-proxiedbean.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/proxiedbean-tc-config.xml";

  private FooService bar1;
  private FooService bar2;

  protected void setUp() throws Exception {
    super.setUp();

    bar1 = (FooService) server1.getProxy(FooService.class, BAR_SERVICE_NAME);
    bar2 = (FooService) server2.getProxy(FooService.class, BAR_SERVICE_NAME);
  }
  
  public void testProxiedBean() throws Exception {
    logger.debug("testing proxied bean");
    assertEquals("0-barAndinterceptorInvoked-rawValue-0", bar1.serviceMethod());
    assertEquals("1-barAndinterceptorInvoked-rawValue-0", bar2.serviceMethod());
  }

  private static class InnerTestSetup extends SpringTwoServerTestSetup {

    private InnerTestSetup() {
      super(SingletonReferencingProxiedBeanTest.class, CONFIG_FILE_FOR_TEST, "test-proxiedbean");
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(BAR_SERVICE_NAME, "service", FooService.class);
      builder.addRemoteService(FOO_SERVICE_NAME, "innerService", FooService.class);
    }
  }

  public static Test suite() {
    return new InnerTestSetup();
  }

}
