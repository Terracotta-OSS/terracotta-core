/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.spring.integrationtests.tests;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tctest.spring.bean.WebFlowBean;
import com.tctest.spring.integrationtests.framework.AbstractTwoServerDeploymentTest;
import com.tctest.spring.integrationtests.framework.Deployment;
import com.tctest.spring.integrationtests.framework.ServerTestSetup;
import com.tctest.spring.integrationtests.framework.WebApplicationServer;

import java.util.Collections;

import junit.framework.Test;


/**
 * Test Spring WebFlow fail-over
 * 
 * LKC-1175: Test: Spring WebFlow fail-over
 * https://jira.terracotta.lan/jira/browse/LKC-1175
 * 
 * Test startup 
 * Test fail-over 
 * More??
 * 
 * Spring Webflow flows are stateful (by nature), they are backed up 
 * by the HttpContext so it will be a no brainer to support. 
 * Need to work out config, if it is needed and if so what it should be.
 * 
 * 1. ContinuationFlowExecutionRepositoryFactory
 * 2. DefaultFlowExecutionRepositoryFactory
 * 3. SingleKeyFlowExecutionRepositoryFactory (extends 2)
 */
public class WebFlowTest extends AbstractTwoServerDeploymentTest {

  protected void tearDown() throws Exception {
    super.tearDown();
    stopAllWebServers();
  }
  
  public static Test suite() {
    return new WebFlowTestSetup();
  }
  
  public void testDefaultFlowExecution() throws Exception {
    checkWebFlow("webflow2.htm", false);
  }
  
  public void testSingleKeyFlowExecution() throws Exception {
    checkWebFlow("webflow3.htm", false);
  }
  
  public void testContinuationFlowExecution() throws Exception {
    checkWebFlow("webflow.htm", true);
  }
  
  
  private void checkWebFlow(String controller, boolean withStepBack) throws Exception {
    server1.start();
    
    WebConversation webConversation1 = new WebConversation();

    Response response1 = request(server1, webConversation1, null, null, controller);
    assertTrue("Expecting non-empty flow execution key; "+response1, response1.flowExecutionKey.length()>0);
    assertEquals("", response1.result);
    
    server2.start();
    
    Response response2 = request(server2, webConversation1, response1.flowExecutionKey, "valueA", controller);
    assertTrue("Expecting non-empty flow execution key; "+response2, response2.flowExecutionKey.length()>0);
    assertEquals("Invalid state; "+response2, WebFlowBean.STATEB, response2.result);
    assertEquals("valueA", response2.valueA);

    Response response3 = request(server1, webConversation1, response2.flowExecutionKey, "valueB", controller);
    assertTrue("Expecting non-empty flow execution key; "+response3, response3.flowExecutionKey.length()>0);
    assertEquals("Invalid state; "+response3, WebFlowBean.STATEC, response3.result);
    assertEquals("Invalid value; "+response3, "valueB", response3.valueB);
    
    server1.stop();
    server2.stop();  // both servers are down
    
    server1.start();
    
    if(withStepBack) {
      // step back
      Response response3a = request(server1, webConversation1, response2.flowExecutionKey, "valueB1", controller);
      assertTrue("Expecting non-empty flow execution key; "+response3a, response3a.flowExecutionKey.length()>0);
      assertEquals("Invalid state; "+response3a, WebFlowBean.STATEC, response3a.result);
      assertEquals("Invalid value; "+response3a, "valueB1", response3a.valueB);
    }
    
    // throw away the step back and continue
    Response response4 = request(server1, webConversation1, response3.flowExecutionKey, "valueC", controller);
    assertTrue("Expecting non-empty flow execution key; "+response4, response4.flowExecutionKey.length()>0);
    assertEquals("Invalid state; "+response4, WebFlowBean.STATED, response4.result);
    assertEquals("Invalid value; "+response4, "valueC", response4.valueC);
    
    server2.start();

    Response response5 = request(server2, webConversation1, response4.flowExecutionKey, "valueD", controller);
    // flowExecutionKey is empty since flow is completed
    assertEquals("Invalid state; "+response5, WebFlowBean.COMPLETE, response5.result);    
    assertEquals("Invalid value; "+response5, "valueD", response5.valueD);
  }
  
  private Response request(WebApplicationServer server, WebConversation conversation, String flowExecutionKey, String value, String controllerName) throws Exception {
    String params = "_eventId=submit";
    if(value!=null) {
      params += "&value="+value;
    }
    if(flowExecutionKey!=null) {
      params += "&_flowExecutionKey="+flowExecutionKey.trim();
    } else {
      params += "&_flowId=webflow";
    }
    
    WebResponse response = server.ping("/webflow/" + controllerName + "?"+params, conversation);
    return new Response(response.getText().trim());
  }


//  public void DONTtestHighlow() throws Exception {
//    if(server1.isStopped()) {
//      server1.start();
//    }
//    
//    WebConversation webConversation1 = new WebConversation();
//
//    String flowExecutionKey = null;
//    int lowGuess = 0;
//    int highGuess = 100;
//    int n = 0;
//    while(n<100) {
//      int guess = (highGuess-lowGuess)/2 + lowGuess;
//      String response = takeGuess(server1, webConversation1, guess, flowExecutionKey);
//      String[] params = response.split(";");
//      assertTrue("Expected at least two parameters: "+response, params.length>1);
//      
//      if(HigherLowerGame.CORRECT.equals(params[0])) {
//        break;
//      } else if(HigherLowerGame.TOO_HIGH.equals(params[0])) {
//        highGuess = guess;  
//      } else if(HigherLowerGame.TOO_LOW.equals(params[0])) {
//        lowGuess = guess;  
//      } else if(HigherLowerGame.INVALID.equals(params[0])) {
//        fail("Invalid execution "+params[1]);
//      }
//
//      flowExecutionKey = params[1];
//    }
//    assertTrue("Too many iterations", n<100);
//  }

//  private String takeGuess(WebApplicationServer server, WebConversation conversation, int newGuess, String flowExecutionKey) throws Exception {
//    String params = "guess="+newGuess;
//    if(flowExecutionKey!=null) {
//      params += "&_flowExecutionKey="+flowExecutionKey.trim();
//    } else {
//      params += "&_flowId=higherlower";
//    }
//    params += "&_eventId=submit";
//    
//    WebResponse response = server.ping("/higherlower/higherlower.htm?"+params, conversation);
//    return response.getText().trim();
//  }
  
  
  private static class WebFlowTestSetup extends ServerTestSetup {

    public WebFlowTestSetup() {
      super(WebFlowTest.class);
    }

    protected void setUp() throws Exception {
      try {
      super.setUp();

//      Deployment deployment1 = new WARBuilder("higherlower.war")
//          .addDirectoryOrJARContainingClass(WebFlowTestSetup.class)
//          .addDirectoryContainingResource("/tc-config-files/webflow-tc-config.xml")
//          
//          .addDirectoryOrJARContainingClass(org.apache.taglibs.standard.Version.class)  // standard-1.0.6.jar
//          .addDirectoryOrJARContainingClass(javax.servlet.jsp.jstl.core.Config.class)  // jstl-1.0.jar
//          .addDirectoryOrJARContainingClass(org.springframework.webflow.registry.XmlFlowRegistryFactoryBean.class)  // spring-webflow-1.0-rc3.jar
//          .addDirectoryOrJARContainingClass(org.springframework.binding.convert.Converter.class)  // spring-binding-1.0-rc3.jar
//          .addDirectoryOrJARContainingClass(org.apache.commons.codec.StringDecoder.class)  // commons-codec-1.3.jar
//          .addDirectoryOrJARContainingClass(ognl.Ognl.class)  // ognl-2.7.jar
//          
//          .addResource("/web-resources", "higherlower.jsp", "/WEB-INF")
//          .addResource("/com/tctest/spring", "higherlower.xml", "/WEB-INF")
//          .addResource("/com/tctest/spring", "higherlower-beans.xml", "/WEB-INF")
//          .addResource("/com/tctest/spring", "higherlower-servlet.xml", "/WEB-INF")
//          
//          .addServlet("higherlower", "*.htm", org.springframework.web.servlet.DispatcherServlet.class, 
//                Collections.singletonMap("contextConfigLocation", "/WEB-INF/higherlower-servlet.xml"), true)
//          .makeDeployment();

      Deployment deployment2 = makeDeploymentBuilder("webflow.war")
          .addDirectoryOrJARContainingClass(WebFlowTestSetup.class)
          .addDirectoryContainingResource("/tc-config-files/webflow-tc-config.xml")
          
          .addDirectoryOrJARContainingClass(org.apache.taglibs.standard.Version.class)  // standard-1.0.6.jar
          .addDirectoryOrJARContainingClass(javax.servlet.jsp.jstl.core.Config.class)  // jstl-1.0.jar
          // .addDirectoryOrJARContainingClass(org.springframework.webflow.registry.XmlFlowRegistryFactoryBean.class)  // spring-webflow-1.0-rc3.jar
          .addDirectoryOrJARContainingClass(org.springframework.webflow.engine.builder.xml.XmlFlowRegistryFactoryBean.class)  // spring-webflow-1.0-rc4.jar
          .addDirectoryOrJARContainingClass(org.springframework.binding.convert.Converter.class)  // spring-binding-1.0-rc3.jar
          .addDirectoryOrJARContainingClass(org.apache.commons.codec.StringDecoder.class)  // commons-codec-1.3.jar
          .addDirectoryOrJARContainingClass(ognl.Ognl.class)  // ognl-2.7.jar
          .addDirectoryOrJARContainingClass(EDU.oswego.cs.dl.util.concurrent.ReentrantLock.class)  // concurrent-1.3.4.jar for SWF on jdk1.4
          
          .addResource("/web-resources", "webflow.jsp", "WEB-INF")
          .addResource("/com/tctest/spring", "webflow.xml", "WEB-INF")
          .addResource("/com/tctest/spring", "webflow-beans.xml", "WEB-INF")
          .addResource("/com/tctest/spring", "webflow-servlet.xml", "WEB-INF")
          .addResource("/web-resources", "weblogic.xml", "WEB-INF")
          
          .addServlet("webflow", "*.htm", org.springframework.web.servlet.DispatcherServlet.class, 
              Collections.singletonMap("contextConfigLocation", "/WEB-INF/webflow-servlet.xml"), true)
          .makeDeployment();
      
      server1 = createServer()
//        .addWarDeployment(deployment1, "higherlower")
        .addWarDeployment(deployment2, "webflow");

      server2 = createServer()
//        .addWarDeployment(deployment1, "higherlower")
        .addWarDeployment(deployment2, "webflow");
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    private WebApplicationServer createServer() throws Exception {
      return sm.makeWebApplicationServer("/tc-config-files/webflow-tc-config.xml");
    }
    
  }
  
  
  private static class Response {
    public final String text;
    public final String flowExecutionKey;
    public final String result;
    public final String valueA;
    public final String valueB;
    public final String valueC;
    public final String valueD;
    
    public Response(String text) {
      this.text = text;
      
      String[] values = text.split(";");
      assertTrue("Expected at least two parameters; "+text, values.length>1);
      
      this.result = values[0];
      this.flowExecutionKey = values[1];
      this.valueA = values.length>2 ? values[2] : null;
      this.valueB = values.length>3 ? values[3] : null;
      this.valueC = values.length>4 ? values[4] : null;
      this.valueD = values.length>5 ? values[5] : null;
    }
    
    public String toString() {
      return text;
    }
    
  }

}

