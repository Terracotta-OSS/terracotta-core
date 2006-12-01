/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.web.context.request.HttpRequestAccessor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.SessionScope;
import org.springframework.web.servlet.DispatcherServlet;

import com.tc.test.TestConfigObject;
import com.tctest.spring.bean.IScopedSimpleBean;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.DeploymentBuilder;
import com.tctest.spring.integrationtests.framework.ProxyBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.extensions.TestSetup;
import junit.framework.Test;


/**
 * Test clustering custom scoped bean. 
 * This custom scope is a subtype of SessionScope with finer granuality establised 
 * specific on request parameter - "CONVERSATION"  
 */
public class CustomScopedBeanTest extends AbstractTwoServerDeploymentTest {

  private static final String SERVICE_FOR_SESSION_SCOPED_BEAN           = "SimpleBeanSvc";
  private static final String BEAN_DEFINITION_FILE_FOR_TEST = "classpath:/com/tctest/spring/beanfactory-customscope.xml";
  private static final String CONFIG_FILE_FOR_TEST          = "/tc-config-files/customscoped-tc-config.xml";


  private static IScopedSimpleBean   beanN1S1C1; // node1 session1 conv1
  private static IScopedSimpleBean   beanN1S1C2; // node1 session1 conv2
  
  private static IScopedSimpleBean   beanN2S1C1; // node2 session1 conv1
  private static IScopedSimpleBean   beanN2S1C2; // node2 session1 conv2
  
  public CustomScopedBeanTest() {
    this.disableVariant(TestConfigObject.SPRING_VARIANT, "128");
  }
  
  public void testSharedFields() throws Exception {
    logger.debug("testing shared fields");
    
    beanN1S1C1.setField("newVal1");   
    Thread.sleep(4000);
    beanN2S1C2.setField("newVal2");

    assertEquals("Failed to share: ", "newVal1", beanN2S1C1.getField());
    assertEquals("Failed to share: ", "newVal2", beanN1S1C2.getField());
    
    logger.debug("!!!! Asserts passed !!!");
  }
  
  public void testScopeId() throws Exception {
    logger.debug("testing scope ids");
    
    String id11 = beanN1S1C1.getScopeId();
    String id12 = beanN1S1C2.getScopeId();

    Thread.sleep(4000);

    String id21 = beanN2S1C1.getScopeId();
    String id22 = beanN2S1C2.getScopeId();
    
    assertEquals("Unexpected scope ids. ", id11, id21);
    assertEquals("Unexpected scope ids. ", id12, id22);
    
    logger.debug("!!!! Asserts passed !!!");    
  }

  public void testDestructionCallbacks() throws Exception {
    logger.debug("testing destruction callbacks");
    
    assertTrue(beanN1S1C1.isInClusteredSingletonCache());
    assertTrue(beanN1S1C2.isInClusteredSingletonCache());
    
    Thread.sleep(4000);
    
    assertTrue(beanN2S1C1.isInClusteredSingletonCache());
    assertTrue(beanN2S1C2.isInClusteredSingletonCache());

    
    beanN1S1C1.invokeDestructionCallback();
    beanN2S1C2.invokeDestructionCallback();
        
    assertFalse("Failed to destruct", beanN1S1C1.isInClusteredSingletonCache());
    assertFalse("Failed to destruct", beanN1S1C2.isInClusteredSingletonCache());
    assertFalse("Failed to destruct", beanN2S1C1.isInClusteredSingletonCache());
    assertFalse("Failed to destruct", beanN2S1C2.isInClusteredSingletonCache());
    
    logger.debug("!!!! Asserts passed !!!");    
  }

  public void testTransparentFields() throws Exception {

    logger.debug("testing transparent fields");
    
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN1S1C1.getTransientField());
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN1S1C2.getTransientField());
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN2S1C1.getTransientField());
    assertEquals("Failed to initialize/virtualize the transient field.", "transient-val", beanN2S1C2.getTransientField());

    
    beanN1S1C1.setTransientField("newVal11");
    beanN1S1C2.setTransientField("newVal12");
    beanN2S1C1.setTransientField("newVal21");
    beanN2S1C2.setTransientField("newVal22");
    
    assertEquals("Unexpected sharing: ", "newVal11", beanN1S1C1.getTransientField());
    assertEquals("Unexpected sharing: ", "newVal12", beanN1S1C2.getTransientField());
    assertEquals("Unexpected sharing: ", "newVal21", beanN2S1C1.getTransientField());
    assertEquals("Unexpected sharing: ", "newVal22", beanN2S1C2.getTransientField());
    
    logger.debug("!!!! Asserts passed !!!");
  }

  private static class InnerTestSetup extends TwoSvrSetup {
    private static final String APP_NAME = "test-customscope";

    private InnerTestSetup() {
      super(CustomScopedBeanTest.class, CONFIG_FILE_FOR_TEST, APP_NAME);
    }

    protected void setUp() throws Exception {
      try {
        super.setUp();
        
        Map attributeMap1 = new HashMap(); attributeMap1.put(ConversationScope.CONV_KEY, "(1)");
        Map attributeMap2 = new HashMap(); attributeMap2.put(ConversationScope.CONV_KEY, "(2)");
        HttpClient clientS1C1 = new HClientWithParams(attributeMap1);
        HttpClient clientS1C2 = new HClientWithParams(attributeMap2);
        
        Map initCtx = new HashMap(); 
        initCtx.put(ProxyBuilder.EXPORTER_TYPE_KEY, HttpInvokerServiceExporter.class);
        
        initCtx.put(ProxyBuilder.HTTP_CLIENT_KEY, clientS1C1);
        
        beanN1S1C1 = (IScopedSimpleBean) server1.getProxy(IScopedSimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SESSION_SCOPED_BEAN, initCtx);
        beanN2S1C1 = (IScopedSimpleBean) server2.getProxy(IScopedSimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SESSION_SCOPED_BEAN, initCtx);
        
        clientS1C2.setState(clientS1C1.getState()); // share state across the clients, they should be in the same session now
        initCtx.put(ProxyBuilder.HTTP_CLIENT_KEY, clientS1C2);
        
        beanN1S1C2 = (IScopedSimpleBean) server1.getProxy(IScopedSimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SESSION_SCOPED_BEAN, initCtx);
        beanN2S1C2 = (IScopedSimpleBean) server2.getProxy(IScopedSimpleBean.class, APP_NAME + "/http/" + SERVICE_FOR_SESSION_SCOPED_BEAN, initCtx);
      } catch (Exception e) {
        e.printStackTrace(); throw e;
      }      
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile(BEAN_DEFINITION_FILE_FOR_TEST);
      builder.addRemoteService(HttpInvokerServiceExporter.class,SERVICE_FOR_SESSION_SCOPED_BEAN, "simplebean", IScopedSimpleBean.class);
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
  
// Inner classes for implementing custom scope and client  
  
  public static class ConversationScope extends SessionScope implements Scope {
    public static final String CONV_KEY = "CONVERSATION";
    
    public String getConversationId() {
      return super.getConversationId() + getWebConversationName();
    }
    
    public Object get(String name, ObjectFactory objectFactory) {
      return super.get(getWebConversationName() + name, 
                       new ObjectFactoryDecorator(objectFactory, this));
    }
    
    public Object remove(String name) {
      return super.remove(getWebConversationName() + name);
    }

    public void registerDestructionCallback(String name, Runnable callback) {
      super.registerDestructionCallback(getWebConversationName() + name, callback);
    }
    
    public Object getDestructionCallback(String name) {
      return RequestContextHolder.currentRequestAttributes().getAttribute(
          ServletRequestAttributes.DESTRUCTION_CALLBACK_NAME_PREFIX 
          + getWebConversationName() + name, RequestAttributes.SCOPE_SESSION);
    }
    
    private String getWebConversationName() {
       String rtv = HttpRequestAccessor.getRequest((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getParameter(CONV_KEY);
       return rtv == null ? "" : rtv;
    }
  }
  
  public static class ObjectFactoryDecorator implements ObjectFactory {
    private ObjectFactory objectFactory;
    private Scope scope;
    
    public ObjectFactoryDecorator(ObjectFactory factory, Scope newScope) {
      this.objectFactory = factory;
      this.scope = newScope;
    }
    
    public Object getObject() throws BeansException {
      Object rtv = objectFactory.getObject();
      if (rtv instanceof ScopeAware) {
        ((ScopeAware)rtv).setScope(scope);
      }
      return rtv;
    }
  }
  
  public interface ScopeAware {
    void setScope(Scope scope);
  }
  
  public static class HClientWithParams extends HttpClient {
    private Map stickyParameters = null;
    
    HClientWithParams(Map attributes) {
      this.stickyParameters = attributes;
    }
    
    public int executeMethod(HostConfiguration hostconfig, 
                             final HttpMethod httpMethod, final HttpState state)
                             throws IOException, HttpException  {
      httpMethod.setQueryString(appendParams(httpMethod.getQueryString()));
      return super.executeMethod(hostconfig, httpMethod, state);
    }
    
    private String appendParams(String queryStr) {
      StringBuffer rtv = new StringBuffer(queryStr==null?"":queryStr);
      if (stickyParameters != null && !stickyParameters.isEmpty()) {
        for (Iterator iter=this.stickyParameters.entrySet().iterator(); iter.hasNext();) {
          if (rtv.length()>0) {
            rtv.append('&');
          }
          Map.Entry entry = (Map.Entry)iter.next();
          rtv.append(entry.getKey()).append('=').append(entry.getValue());
        }

      }
      return rtv.toString();
    }
  }
}
