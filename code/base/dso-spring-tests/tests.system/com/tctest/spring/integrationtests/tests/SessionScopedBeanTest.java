/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.web.servlet.DispatcherServlet;

import com.tc.test.TestConfigObject;
import com.tctest.spring.bean.ISimpleBean;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.ProxyBuilder;

import java.util.HashMap;
import java.util.Map;

import junit.extensions.TestSetup;
import junit.framework.Test;


/**
 * Test clustering session scoped bean.
 */
public class SessionScopedBeanTest extends AbstractTwoServerDeploymentTest {

  private static final String SERVICE_FOR_SHARED_SESSION_SCOPED_BEAN           = "SimpleBeanSvc";
//  private static final String SERVICE_FOR_LOCAL_SESSION_SCOPED_BEAN           = "LocalSimpleBeanSvc";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-sessionscope.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/sessionscoped-tc-config.xml";


  private static ISimpleBean   beanN1S1; // node1 session1
  private static ISimpleBean   beanN1S2; // node1 session2
  
  private static ISimpleBean   beanN2S1; // node2 session1
  private static ISimpleBean   beanN2S2; // node2 session2
  
//  private static ISimpleBean   beanN1S1Local; // node1 session1
//  private static ISimpleBean   beanN2S1Local; // node2 session1

  public SessionScopedBeanTest() {
    this.disableVariant(TestConfigObject.SPRING_VARIANT, "128");
  }
  
//  public void testLocalSessionScopedBeans() throws Exception {
//    logger.debug("testing local beans");
//    
//    beanN1S1Local.setField("newLocalVal1");
//    
//    Thread.sleep(4000);
//    
//    assertEquals("Unexcpected value: ", "newLocalVal1", beanN1S1Local.getField());
//    assertEquals("Unexcpected sharing: ", "local-v1", beanN2S1Local.getField());
//    
//    logger.debug("!!!! Asserts passed !!!");
//  }

  public void testSharedFields() throws Exception {
    beanN1S1.setField("newVal1");
    beanN2S2.setField("newVal2");
    
    Thread.sleep(4000);
    
    assertEquals("Failed to shared: ", "newVal1", beanN2S1.getField());
    assertEquals("Failed to shared: ", "newVal2", beanN1S2.getField());
  }

  public void testTransparentFields() throws Exception {
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN1S1.getTransientField());
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN1S2.getTransientField());
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN2S1.getTransientField());
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN2S2.getTransientField());

    beanN1S1.setTransientField("newVal11");
    beanN1S2.setTransientField("newVal12");
    beanN2S1.setTransientField("newVal21");
    beanN2S2.setTransientField("newVal22");
    
    assertEquals("Unexpected sharing: ", "newVal11", beanN1S1.getTransientField());
    assertEquals("Unexpected sharing: ", "newVal12", beanN1S2.getTransientField());
    assertEquals("Unexpected sharing: ", "newVal21", beanN2S1.getTransientField());
    assertEquals("Unexpected sharing: ", "newVal22", beanN2S2.getTransientField());
  }

  private static class InnerTestSetup extends TwoSvrSetup {
    private static final String APP_NAME = "test-sessionscope";

    private InnerTestSetup() {
      super(SessionScopedBeanTest.class, CONFIG_FILE_FOR_TEST, APP_NAME);
    }

    protected void setUp() throws Exception {
      try {
        super.setUp();
        
        Map initCtx = new HashMap(); 
        initCtx.put(ProxyBuilder.EXPORTER_TYPE_KEY, HttpInvokerServiceExporter.class);
        
        beanN1S1 = (ISimpleBean) server1.getProxy(ISimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SHARED_SESSION_SCOPED_BEAN, initCtx);
//        beanN1S1Local = (ISimpleBean) server1.getProxy(ISimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_LOCAL_SESSION_SCOPED_BEAN);
        beanN2S1 = (ISimpleBean) server2.getProxy(ISimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SHARED_SESSION_SCOPED_BEAN, initCtx);
//        beanN2S1Local = (ISimpleBean) server2.getProxy(ISimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_LOCAL_SESSION_SCOPED_BEAN);
        
        initCtx.remove(ProxyBuilder.HTTP_CLIENT_KEY); // this resets the internal client
        
        beanN1S2 = (ISimpleBean) server1.getProxy(ISimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SHARED_SESSION_SCOPED_BEAN, initCtx);
        beanN2S2 = (ISimpleBean) server2.getProxy(ISimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SHARED_SESSION_SCOPED_BEAN, initCtx);
      } catch (Exception e) {
        e.printStackTrace(); throw e;
      }      
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(HttpInvokerServiceExporter.class,SERVICE_FOR_SHARED_SESSION_SCOPED_BEAN, "simplebean", ISimpleBean.class);
//      builder.addRemoteService(HttpInvokerServiceExporter.class,SERVICE_FOR_LOCAL_SESSION_SCOPED_BEAN, "localsimplebean", ISimpleBean.class);
      builder.setDispatcherServlet("httpinvoker", "/http/*", DispatcherServlet.class, null, true);
      builder.addDirectoryOrJARContainingClass(net.sf.cglib.core.Constants.class);
    }

  }

  /**
   * JUnit test loader entry point
   */
  public static Test suite() {
    TestSetup setup = new InnerTestSetup();
    return setup;
  }

}
