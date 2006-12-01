/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.web.servlet.DispatcherServlet;

import com.tctest.spring.bean.ISingleton;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.ProxyBuilder;

import java.util.HashMap;
import java.util.Map;

import junit.extensions.TestSetup;
import junit.framework.Test;


/**
 * Serve as an example for using the HttpInvoker framework.
 */
public class SingletonV3Test extends AbstractTwoServerDeploymentTest {

  private static final String REMOTE_SERVICE_NAME           = "singletonservice";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/singleton-tc-config.xml";


  private static ISingleton   singleton1;
  private static ISingleton   singleton2;

  public void testSharedField() throws Exception {

    logger.debug("testing shared fields");

    assertEquals(singleton1.getCounter(), singleton2.getCounter());
    singleton1.incrementCounter();
    assertEquals(singleton1.getCounter(), singleton2.getCounter());
    singleton2.incrementCounter();
    assertEquals(singleton2.getCounter(), singleton1.getCounter());

    logger.debug("!!!! Asserts passed !!!");
  }

  public void testTransientField() throws Exception {

    logger.debug("Testing transient fields");
    assertEquals("aaa", singleton1.getTransientValue());
    assertEquals("aaa", singleton2.getTransientValue());
    singleton1.setTransientValue("s1");
    assertEquals("aaa", singleton2.getTransientValue());
    singleton2.setTransientValue("s2");
    assertEquals("s1", singleton1.getTransientValue());
    assertEquals("s2", singleton2.getTransientValue());
    logger.debug("done testing transient fields");
  }

  public void testSharedBooleanField() throws Exception {
    assertTrue(singleton1.toggleBoolean());
    assertFalse(singleton2.toggleBoolean());
    assertTrue(singleton1.toggleBoolean());
  }

  private static class SingletonTestSetup extends TwoSvrSetup {
    private static final String APP_NAME = "test-singleton";

    private SingletonTestSetup() {
      super(SingletonV3Test.class, CONFIG_FILE_FOR_TEST, APP_NAME);
    }

    protected void setUp() throws Exception {
      try {
        super.setUp();
        
        Map initCtx = new HashMap(); 
        initCtx.put(ProxyBuilder.EXPORTER_TYPE_KEY, HttpInvokerServiceExporter.class);
        singleton1 = (ISingleton) server1.getProxy(ISingleton.class, APP_NAME + "/http/" + REMOTE_SERVICE_NAME, initCtx);
        singleton2 = (ISingleton) server2.getProxy(ISingleton.class, APP_NAME + "/http/" + REMOTE_SERVICE_NAME, initCtx);
      } catch(Exception ex) {
        ex.printStackTrace(); throw ex;
      }
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(HttpInvokerServiceExporter.class,REMOTE_SERVICE_NAME, "singleton", ISingleton.class);
      builder.setDispatcherServlet("httpinvoker", "/http/*", DispatcherServlet.class, null, true);
    }

  }

  /**
   * JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new SingletonTestSetup();
    return setup;
  }

}
