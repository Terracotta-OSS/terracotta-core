/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

package com.tctest.spring.integrationtests.tests;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.spring.integrationtests.SpringTwoServerTestSetup;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import junit.framework.Test;


/**
 * Test Spring WebFlow Sellitem test
 * 
 * LKC-2369: Nested lock exception with TCSpring clustering of sample apps
 * https://jira.terracotta.lan/jira/browse/LKC-2369
 */
public class WebFlowSellItemTest extends AbstractTwoServerDeploymentTest {

  public static Test suite() {
    return new WebFlowTestSetup();
  }

  
  protected void tearDown() throws Exception {
    super.tearDown();
    stopAllWebServers();
  }
  
  public void testSellitem() throws Exception {

    server0.start();
    server1.start();
    
    WebConversation webConversation1 = new WebConversation();
    // XXX interleave servers when LKC-2369 is fixed
    executeConversation(webConversation1, server0, server1, server1, server0);
    
    // XXX Uncomment when LKC-2369 is fixed 
    WebConversation webConversation2 = new WebConversation();
    executeConversation(webConversation2, server0, server1, server0, server1);
    
  }

  private void executeConversation(WebConversation webConversation1, 
         WebApplicationServer s1, WebApplicationServer s2, WebApplicationServer s3, WebApplicationServer s4) throws Exception {
    // priceAndItemCountForm.jsp
    Properties response1 = request(s1, webConversation1, null, null);
    assertProperty(response1, "form", "priceAndItemCountForm");
    assertProperty(response1, "flowExecutionKey");
    assertProperty(response1, "price");
    assertProperty(response1, "itemCount");
    
    String flowExecutionKey2 = response1.getProperty("flowExecutionKey");
    
    Map map2 = new HashMap();
    map2.put("price", "101.5");
    map2.put("itemCount", "500");
    map2.put("_eventId_submit", "Next");
    // categoryForm.jsp
    Properties response2 = request(s2, webConversation1, flowExecutionKey2, map2);
    assertProperty(response2, "form", "categoryForm");
    assertProperty(response2, "flowExecutionKey");
    assertProperty(response2, "price", "101.5");
    assertProperty(response2, "itemCount", "500");

    String flowExecutionKey3 = response2.getProperty("flowExecutionKey");

    Map map3 = new HashMap();
    map3.put("shipping", "true");
    map3.put("category", "B");
    map3.put("_eventId_submit", "Next");
    // shippingDetailsForm.jsp
    Properties response3 = request(s3, webConversation1, flowExecutionKey3, map3);
    assertProperty(response3, "form", "shippingDetailsForm");
    assertProperty(response3, "flowExecutionKey");
    assertProperty(response2, "price", "101.5");
    assertProperty(response2, "itemCount", "500");
    assertProperty(response3, "category", "B");
    assertProperty(response3, "shipping", "true");

    String flowExecutionKey4 = response3.getProperty("flowExecutionKey");
    
    Map map4 = new HashMap();
    map4.put("_eventId_submit", "Next");
    map4.put("statusValue", "E");
    // costOverview
    Properties response4 = request(s4, webConversation1, flowExecutionKey4, map4);
    assertProperty(response4, "form", "costOverview");
    // assertProperty(response4, "flowExecutionKey");
    assertProperty(response4, "totalCost");
  }
  
  private void assertProperty(Properties properties, String name) {
    assertNotNull("No properties returned; "+ properties, properties);
    String value = properties.getProperty(name);
    assertNotNull("Missing property "+name+"; "+ properties, value);
    assertTrue("Expecting non-empty "+name+"; "+properties, value.trim().length()>0);
  }

  private void assertProperty(Properties properties, String name, String expected) {
    assertNotNull("No properties returned; "+ properties, properties);
    String value = properties.getProperty(name);
    assertEquals("Invalid "+name+"; "+properties, expected, value);
  }
  
  private Properties request(WebApplicationServer server, WebConversation conversation, String flowExecutionKey, Map map) throws Exception {
    String params = "";
    if(map==null) {
      params += "_eventId=submit";
    } else {
      for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        params += "&"+entry.getKey()+"="+entry.getValue();
      }
    }
    if(flowExecutionKey!=null) {
      params += "&_flowExecutionKey="+flowExecutionKey.trim();
    } else {
      params += "&_flowId=sellitem";
    }
    
    WebResponse response = server.ping("/sellitem/pos.htm?"+params, conversation);
    
    Properties properties = new Properties();
    properties.load(new ByteArrayInputStream(response.getText().getBytes("ASCII")));
    logger.info("*** "+properties);
    return properties;
  }


  private static class WebFlowTestSetup extends SpringTwoServerTestSetup {

    public WebFlowTestSetup() {
      super(WebFlowSellItemTest.class, "/tc-config-files/sellitem-tc-config.xml", "sellitem");
      setStart(false);
    }

    protected void configureWar(DeploymentBuilder builder) {
      // .addDirectoryOrJARContainingClass(WebFlowTestSetup.class)
      // .addDirectoryContainingResource("/tc-config-files/sellitem-tc-config.xml")
      builder.addDirectoryContainingResource("/com/tctest/spring/sellitem-ctx.xml");
      
      builder.addDirectoryOrJARContainingClass(org.apache.taglibs.standard.Version.class);  // standard-1.0.6.jar
      builder.addDirectoryOrJARContainingClass(javax.servlet.jsp.jstl.core.Config.class);  // jstl-1.0.jar
      // .addDirectoryOrJARContainingClass(org.springframework.webflow.registry.XmlFlowRegistryFactoryBean.class)  // spring-webflow-1.0-rc3.jar
      builder.addDirectoryOrJARContainingClass(org.springframework.webflow.engine.builder.xml.XmlFlowRegistryFactoryBean.class);  // spring-webflow-1.0-rc4.jar
      builder.addDirectoryOrJARContainingClass(org.springframework.binding.convert.Converter.class);  // spring-binding-1.0-rc3.jar
      builder.addDirectoryOrJARContainingClass(org.apache.commons.codec.StringDecoder.class);  // commons-codec-1.3.jar
      builder.addDirectoryOrJARContainingClass(ognl.Ognl.class);  // ognl-2.7.jar
      builder.addDirectoryOrJARContainingClass(EDU.oswego.cs.dl.util.concurrent.ReentrantLock.class);  // concurrent-1.3.4.jar for SWF on jdk1.4

      builder.addDirectoryOrJARContainingClass(org.hsqldb.jdbcDriver.class);  // hsqldb*.jar
      
      builder.addResource("/web-resources/sellitem", "categoryForm.jsp", "WEB-INF/jsp");
      builder.addResource("/web-resources/sellitem", "costOverview.jsp", "WEB-INF/jsp");
      builder.addResource("/web-resources/sellitem", "error.jsp", "WEB-INF/jsp");
      builder.addResource("/web-resources/sellitem", "includeTop.jsp", "WEB-INF/jsp");
      builder.addResource("/web-resources/sellitem", "priceAndItemCountForm.jsp", "WEB-INF/jsp");
      builder.addResource("/web-resources/sellitem", "shippingDetailsForm.jsp", "WEB-INF/jsp");
      builder.addResource("/web-resources", "weblogic.xml", "/WEB-INF");
      
      builder.addResource("/com/tctest/spring", "sellitem.xml", "WEB-INF");
      builder.addResource("/com/tctest/spring", "sellitem-shipping.xml", "WEB-INF");
      builder.addResource("/com/tctest/spring", "sellitem-beans.xml", "WEB-INF");
      builder.addResource("/com/tctest/spring", "sellitem-servlet.xml", "WEB-INF");
      
      builder.addServlet("sellitem", "*.htm", org.springframework.web.servlet.DispatcherServlet.class, 
          Collections.singletonMap("contextConfigLocation", 
              "/WEB-INF/sellitem-servlet.xml\n" +
              "classpath:com/tctest/spring/sellitem-ctx.xml"), true);
    }

    
//    protected void setUp() throws Exception {
//      super.setUp();
//
//      Deployment deployment2 = makeDeploymentBuilder("sellitem.war")
//          .addDirectoryOrJARContainingClass(WebFlowTestSetup.class)
//          .addDirectoryContainingResource("/tc-config-files/sellitem-tc-config.xml")
//          .addDirectoryContainingResource("/com/tctest/spring/sellitem-ctx.xml")
//          
//          .addDirectoryOrJARContainingClass(org.apache.taglibs.standard.Version.class)  // standard-1.0.6.jar
//          .addDirectoryOrJARContainingClass(javax.servlet.jsp.jstl.core.Config.class)  // jstl-1.0.jar
//          // .addDirectoryOrJARContainingClass(org.springframework.webflow.registry.XmlFlowRegistryFactoryBean.class)  // spring-webflow-1.0-rc3.jar
//          .addDirectoryOrJARContainingClass(org.springframework.webflow.engine.builder.xml.XmlFlowRegistryFactoryBean.class)  // spring-webflow-1.0-rc4.jar
//          .addDirectoryOrJARContainingClass(org.springframework.binding.convert.Converter.class)  // spring-binding-1.0-rc3.jar
//          .addDirectoryOrJARContainingClass(org.apache.commons.codec.StringDecoder.class)  // commons-codec-1.3.jar
//          .addDirectoryOrJARContainingClass(ognl.Ognl.class)  // ognl-2.7.jar
//          .addDirectoryOrJARContainingClass(EDU.oswego.cs.dl.util.concurrent.ReentrantLock.class)  // concurrent-1.3.4.jar for SWF on jdk1.4
//
//          .addDirectoryOrJARContainingClass(org.hsqldb.jdbcDriver.class)  // hsqldb*.jar
//          
//          .addResource("/web-resources/sellitem", "categoryForm.jsp", "WEB-INF/jsp")
//          .addResource("/web-resources/sellitem", "costOverview.jsp", "WEB-INF/jsp")
//          .addResource("/web-resources/sellitem", "error.jsp", "WEB-INF/jsp")
//          .addResource("/web-resources/sellitem", "includeTop.jsp", "WEB-INF/jsp")
//          .addResource("/web-resources/sellitem", "priceAndItemCountForm.jsp", "WEB-INF/jsp")
//          .addResource("/web-resources/sellitem", "shippingDetailsForm.jsp", "WEB-INF/jsp")
//          .addResource("/web-resources", "weblogic.xml", "/WEB-INF")
//          
//          .addResource("/com/tctest/spring", "sellitem.xml", "WEB-INF")
//          .addResource("/com/tctest/spring", "sellitem-shipping.xml", "WEB-INF")
//          .addResource("/com/tctest/spring", "sellitem-beans.xml", "WEB-INF")
//          .addResource("/com/tctest/spring", "sellitem-servlet.xml", "WEB-INF")
//          
//          .addServlet("sellitem", "*.htm", org.springframework.web.servlet.DispatcherServlet.class, 
//              Collections.singletonMap("contextConfigLocation", 
//                  "/WEB-INF/sellitem-servlet.xml\n" +
//                  "classpath:com/tctest/spring/sellitem-ctx.xml"), true)
//          .makeDeployment();
//      
//      server1 = createServer(deployment2);
//      server2 = createServer(deployment2);
//    }
//
//    private WebApplicationServer createServer(Deployment deployment) throws Exception {
//      return sm.makeWebApplicationServer("/tc-config-files/sellitem-tc-config.xml")
//          .addWarDeployment(deployment, "sellitem");
//    }
    
  }

}

