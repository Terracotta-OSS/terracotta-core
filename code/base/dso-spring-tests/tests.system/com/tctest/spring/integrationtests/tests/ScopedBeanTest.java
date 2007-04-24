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

import junit.framework.Test;


/**
 * Test ApplicationContext definition
 * 
 * LKC-804: Test: Session scoped Spring Beans
 * https://jira.terracotta.lan/jira//browse/LKC-804
 * 
 * Test sharing of state 
 * Test scope is really session scope 
 * More??
 * 
 * Use Tomcat and WLS DSO session clustering.
 * 
 * <b>This is for Spring 2.0 only!</b>
 */
public class ScopedBeanTest extends AbstractTwoServerDeploymentTest {

  public static Test suite() {
    return new SessionScopedBeanTestSetup();
  }

  
  public ScopedBeanTest() {
    // XXX timebombed till 2006-09-15
    disableAllUntil("2007-09-15");
  }
  
  public void testSessionScopedBean() throws Exception {
    WebConversation webConversation1 = new WebConversation();    
    verifyValue(server1, webConversation1, "Jonas");
    updateValue(server1, webConversation1, "Tim");
    verifyValue(server1, webConversation1, "Tim");

    WebConversation webConversation2 = new WebConversation();    
    verifyValue(server2, webConversation2, "Jonas");
    updateValue(server2, webConversation2, "Steve");
    verifyValue(server2, webConversation2, "Steve");
    verifyValue(server1, webConversation2, "Steve");

    verifyValue(server2, webConversation1, "Tim");
    verifyValue(server2, webConversation1, "Tim");
  }


  private void verifyValue(WebApplicationServer server, WebConversation conversation, String expectedText) throws Exception {
    WebResponse response = server.ping("/scopedBeans/get.html", conversation);
    String responseText = response.getText().trim();
    assertEquals("Incorrect value from session "+conversation.getCookieValue("JSESSIONID"), expectedText, responseText);
  }

  private void updateValue(WebApplicationServer server, WebConversation conversation, String newText) throws Exception {
    WebResponse response = server.ping("/scopedBeans/set.html?value="+newText, conversation);
    String responseText = response.getText().trim();
    assertEquals("Incorrect value from session "+conversation.getCookieValue("JSESSIONID"), newText, responseText);
  }
  
  
  private static class SessionScopedBeanTestSetup extends SpringTwoServerTestSetup {

    public SessionScopedBeanTestSetup() {
      super(ScopedBeanTest.class, "/tc-config-files/scopedbeans-tc-config.xml", "scopedBeans");
    }
    

//    protected void setUp() throws Exception {
//      super.setUp();
//
//      Deployment deployment = makeDeploymentBuilder("scopedBeans.war")
//          .addDirectoryOrJARContainingClass(SessionScopedBeanTestSetup.class)
//          .addDirectoryOrJARContainingClass(org.apache.taglibs.standard.Version.class)  // standard-1.0.6.jar
//          .addDirectoryOrJARContainingClass(javax.servlet.jsp.jstl.core.Config.class)  // jstl-1.0.jar
//
//          .addDirectoryContainingResource("/tc-config-files/scopedbeans-tc-config.xml")
//
//          .addResource("/web-resources", "scopedBeans.jsp", "WEB-INF")
//          .addResource("/web-resources", "weblogic.xml", "WEB-INF")
//
//          .addResource("/com/tctest/spring", "scopedBeans-servlet.xml", "WEB-INF")
//
//          .addServlet("scopedBeans", "*.html", org.springframework.web.servlet.DispatcherServlet.class, null, true)
//          
//          .makeDeployment();
//
//      server1 = createServer(deployment);
//      server2 = createServer(deployment);
//    }
//
//    private WebApplicationServer createServer(Deployment deployment) throws Exception {
//      WebApplicationServer server = sm.makeWebApplicationServer("/tc-config-files/scopedbeans-tc-config.xml");
//      
//      server.addWarDeployment(deployment, "scopedBeans");
//      server.start();
//      
//      return server;
//    }


    protected void configureWar(DeploymentBuilder builder) {
      builder
        // .addDirectoryOrJARContainingClass(SessionScopedBeanTestSetup.class)
        .addDirectoryOrJARContainingClass(org.apache.taglibs.standard.Version.class)  // standard-1.0.6.jar
        .addDirectoryOrJARContainingClass(javax.servlet.jsp.jstl.core.Config.class)  // jstl-1.0.jar

        .addResource("/web-resources", "scopedBeans.jsp", "WEB-INF")
        .addResource("/web-resources", "weblogic.xml", "WEB-INF")

        .addResource("/com/tctest/spring", "scopedBeans-servlet.xml", "WEB-INF")

        .addServlet("scopedBeans", "*.html", org.springframework.web.servlet.DispatcherServlet.class, null, true);
      
    }
    
  }

}

