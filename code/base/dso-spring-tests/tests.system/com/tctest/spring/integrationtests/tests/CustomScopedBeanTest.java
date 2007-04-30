/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.tests;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
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
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ProxyBuilder;
import com.tcspring.ComplexBeanId;
import com.tcspring.DistributableBeanFactory;
import com.tctest.spring.bean.ISimpleBean;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSessionBindingListener;

import junit.framework.Test;


/**
 * Test clustering custom scoped bean. This custom scope is a subtype of SessionScope with finer granularity driven by
 * CONVERSATION parameter of the http request
 */
public class CustomScopedBeanTest extends AbstractTwoServerDeploymentTest {
  private static final String APP_NAME = "test-customscope";

  private static final String FASADE_NAME = "TestFasadeService";
  
  private ITestFacade beanN1C1; // node1 session1 conv1
  private ITestFacade beanN1C2; // node1 session1 conv2
  
  private ITestFacade beanN2C1; // node2 session1 conv1
  private ITestFacade beanN2C2; // node2 session1 conv2
  
  
  public CustomScopedBeanTest() {
    this.disableAllUntil("2007-05-14");
    this.disableVariant(TestConfigObject.SPRING_VARIANT, "128");
  }
  
  protected void setUp() throws Exception {
    try {
      super.setUp();
      
      Map initCtx = new HashMap(); 
      initCtx.put(ProxyBuilder.EXPORTER_TYPE_KEY, HttpInvokerServiceExporter.class);
      
      HttpClient clientS1C1 = new HttpClientWithParams(Collections.singletonMap(ConversationScope.CONV_KEY, "(1)"));
      initCtx.put(ProxyBuilder.HTTP_CLIENT_KEY, clientS1C1);
      
      beanN1C1 = (ITestFacade) server1.getProxy(ITestFacade.class, APP_NAME + "/http/" + FASADE_NAME, initCtx);
      beanN2C1 = (ITestFacade) server2.getProxy(ITestFacade.class, APP_NAME + "/http/" + FASADE_NAME, initCtx);
      
      HttpClient clientS1C2 = new HttpClientWithParams(Collections.singletonMap(ConversationScope.CONV_KEY, "(2)"));
      clientS1C2.setState(clientS1C1.getState()); // share state across the clients, they should be in the same session now
      initCtx.put(ProxyBuilder.HTTP_CLIENT_KEY, clientS1C2);
      
      beanN1C2 = (ITestFacade) server1.getProxy(ITestFacade.class, APP_NAME + "/http/" + FASADE_NAME, initCtx);
      beanN2C2 = (ITestFacade) server2.getProxy(ITestFacade.class, APP_NAME + "/http/" + FASADE_NAME, initCtx);
    } catch (Exception e) {
      e.printStackTrace(); 
      throw e;
    }      
  }

  
  public void testSharedFields() throws Exception {
    beanN1C1.setField("newVal1");   
    beanN2C2.setField("newVal2");

    assertEquals("Failed to share", "newVal1", beanN2C1.getField());
    assertEquals("Failed to share", "newVal2", beanN1C2.getField());
  }
  
  public void testScopeId() throws Exception {
    String id11 = beanN1C1.getConversationId();
    String id12 = beanN1C2.getConversationId();

    String id21 = beanN2C1.getConversationId();
    String id22 = beanN2C2.getConversationId();
    
    assertEquals("Unexpected scope", id11, id21);
    assertEquals("Unexpected scope", id12, id22);
  }

  public void testDestructionCallbacks() throws Exception {
    String conversationId11 = beanN1C1.getConversationId();
    String conversationId21 = beanN1C2.getConversationId();
    String conversationId12 = beanN2C1.getConversationId();
    String conversationId22 = beanN2C2.getConversationId();

    beanN1C1.setField("newVal11");   
    beanN1C2.setField("newVal12");
    beanN2C1.setField("newVal21");   
    beanN2C2.setField("newVal22");
    
    assertTrue("Failed to create scoped bean", beanN1C1.isInClusteredSingletonCache(conversationId11));
    assertTrue("Failed to create scoped bean", beanN1C2.isInClusteredSingletonCache(conversationId12));
    assertTrue("Failed to create scoped bean", beanN2C1.isInClusteredSingletonCache(conversationId21));
    assertTrue("Failed to create scoped bean", beanN2C2.isInClusteredSingletonCache(conversationId22));
    
    beanN1C1.invokeDestructionCallback();
    beanN2C2.invokeDestructionCallback();
        
    assertFalse("Failed to destruct scoped bean", beanN1C1.isInClusteredSingletonCache(conversationId11));
    assertFalse("Failed to destruct scoped bean", beanN1C2.isInClusteredSingletonCache(conversationId12));
    assertFalse("Failed to destruct scoped bean", beanN2C1.isInClusteredSingletonCache(conversationId21));
    assertFalse("Failed to destruct scoped bean", beanN2C2.isInClusteredSingletonCache(conversationId22));
  }

  public void testTransparentFields() throws Exception {
    assertEquals("Failed to initialize transient field", "transient-val", beanN1C1.getTransientField());
    assertEquals("Failed to initialize transient field", "transient-val", beanN1C2.getTransientField());
    assertEquals("Failed to initialize transient field", "transient-val", beanN2C1.getTransientField());
    assertEquals("Failed to initialize transient field", "transient-val", beanN2C2.getTransientField());
    
    beanN1C1.setTransientField("newVal11");
    beanN1C2.setTransientField("newVal12");
    beanN2C1.setTransientField("newVal21");
    beanN2C2.setTransientField("newVal22");
    
    assertEquals("Unexpected sharing", "newVal11", beanN1C1.getTransientField());
    assertEquals("Unexpected sharing", "newVal12", beanN1C2.getTransientField());
    assertEquals("Unexpected sharing", "newVal21", beanN2C1.getTransientField());
    assertEquals("Unexpected sharing", "newVal22", beanN2C2.getTransientField());
  }

  
  private static class InnerTestSetup extends SpringTwoServerTestSetup {

    private InnerTestSetup() {
      super(CustomScopedBeanTest.class, "/tc-config-files/customscoped-tc-config.xml", APP_NAME);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addBeanDefinitionFile("classpath:/com/tctest/spring/beanfactory-customscope.xml");
      
      builder.addRemoteService(HttpInvokerServiceExporter.class, FASADE_NAME, "testFacade", ITestFacade.class);

      builder.setDispatcherServlet("httpinvoker", "/http/*", DispatcherServlet.class, null, true);
      builder.addDirectoryOrJARContainingClass(net.sf.cglib.core.Constants.class);
    }
  }


  public static Test suite() {
    return new InnerTestSetup();
  }
  
  
  /**
   * Custom scope and client  
   */
  public static class ConversationScope extends SessionScope implements Scope {
    public static final String CONV_KEY = "CONVERSATION";
    
    public String getConversationId() {
      String conversation = getWebConversationName();
      return conversation == null ? null : super.getConversationId() + conversation;
    }
    
    public Object get(String name, ObjectFactory objectFactory) {
      return super.get(getWebConversationName() + name, objectFactory);
    }
    
    public Object remove(String name) {
      return super.remove(getWebConversationName() + name);
    }

    public void registerDestructionCallback(String name, Runnable callback) {
      super.registerDestructionCallback(getWebConversationName() + name, callback);
    }
    
    public Object getDestructionCallback(String name) {
      return RequestContextHolder.currentRequestAttributes().getAttribute(
          ServletRequestAttributes.DESTRUCTION_CALLBACK_NAME_PREFIX + getWebConversationName() + name, 
          RequestAttributes.SCOPE_SESSION);
    }
    
    private String getWebConversationName() {
      return HttpRequestAccessor.getRequest(
           (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getParameter(CONV_KEY);
    }
  }
  
  
  public static class HttpClientWithParams extends HttpClient {
    private Map stickyParameters = null;

    HttpClientWithParams(Map attributes) {
      this.stickyParameters = attributes;
    }

    public int executeMethod(HostConfiguration hostconfig, final HttpMethod httpMethod, final HttpState state)
        throws IOException, HttpException {
      httpMethod.setQueryString(appendParams(httpMethod.getQueryString()));
      return super.executeMethod(hostconfig, httpMethod, state);
    }

    private String appendParams(String queryStr) {
      StringBuffer sb = new StringBuffer(queryStr == null ? "" : queryStr);
      if (stickyParameters != null) {
        for (Iterator iter = this.stickyParameters.entrySet().iterator(); iter.hasNext();) {
          if (sb.length() > 0) {
            sb.append('&');
          }
          Map.Entry entry = (Map.Entry) iter.next();
          sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
      }
      return sb.toString();
    }
  }
  
  
  public static interface ITestFacade {
    public String getConversationId();
    public String getTransientField();
    public void setTransientField(String string);
    public String getField();
    public void setField(String value);
    public void invokeDestructionCallback();
    public boolean isInClusteredSingletonCache(String conversationId);
  }
  
  
  public static class ConversationScopeTestFacade implements ITestFacade, BeanFactoryAware {
    private BeanFactory factory;
    private ConversationScope scope;
    private ISimpleBean bean;
    private String beanName;
    
    public void setBeanFactory(BeanFactory factory) {
      this.factory = factory;
    }
    
    public void setScope(ConversationScope scope) {
      this.scope = scope;
    }

    public void setBean(ISimpleBean bean) {
      this.bean = bean;
    }
    
    public void setField(String value) {
      bean.setField(value);
    }
    
    public String getField() {
      return bean.getField();
    }
    
    public String getTransientField() {
      return bean.getTransientField();
    }
    
    public void setTransientField(String value) {
      bean.setTransientField(value);
    }
    
    public String getConversationId() {
      return scope.getConversationId();
    }
    
    public void invokeDestructionCallback() {
      System.err.println("#### ConversationScopeTestFacade.invokeDestructionCallback() " + getBeanName());
      HttpSessionBindingListener listener = (HttpSessionBindingListener) scope.getDestructionCallback(getBeanName());
      listener.valueUnbound(null); // cause unbound
    }
    
    public boolean isInClusteredSingletonCache(String conversationId) {
      ComplexBeanId beanId = new ComplexBeanId(conversationId, getBeanName());
      boolean res = ((DistributableBeanFactory) factory).getBeanContainer(beanId) != null;
      System.err.println("#### ConversationScopeTestFacade.isInClusteredSingletonCache() " + beanId + " " + res);
      return res;
    }

    private String getBeanName() {
      if(beanName==null) {
        beanName = bean.getBeanName();
      }
      return beanName;
    }

  }
  
}
