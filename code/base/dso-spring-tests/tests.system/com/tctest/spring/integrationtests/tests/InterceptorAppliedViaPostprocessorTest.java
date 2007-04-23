/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tctest.spring.bean.FooService;

import junit.extensions.TestSetup;
import junit.framework.Test;

/**
 * Verify that an interceptor is applied via a postprocessor to a distributed bean
 */
public class InterceptorAppliedViaPostprocessorTest extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "FooService";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/interceptor-via-postprocessor.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/interceptor-via-postprocessor-tc-config.xml";

  private static FooService   singleton1;
  private static FooService   singleton2;

  public void testInterceptor() throws Exception {

    assertEquals("interceptorInvoked-rawValue-0", singleton1.serviceMethod());
    assertEquals("interceptorInvoked-rawValue-1", singleton2.serviceMethod());

  }

  private static class TS extends TwoSvrSetup {
    private TS() {
      super(InterceptorAppliedViaPostprocessorTest.class, CONFIG_FILE_FOR_TEST, "test-interceptor");
    }

    protected void setUp() throws Exception {
      super.setUp();

      singleton1 = (FooService) server1.getProxy(FooService.class, REMOTE_SERVICE_NAME);
      singleton2 = (FooService) server2.getProxy(FooService.class, REMOTE_SERVICE_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(REMOTE_SERVICE_NAME, "service", FooService.class);
    }

  }

  /**
   * JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new TS();
    return setup;
  }

}
