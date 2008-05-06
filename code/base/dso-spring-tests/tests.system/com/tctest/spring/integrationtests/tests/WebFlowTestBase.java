/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tctest.spring.integrationtests.tests;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tctest.spring.bean.WebFlowBean;
import com.tctest.spring.integrationtests.SpringDeploymentTest;

import java.util.Collections;

/**
 * Test Spring WebFlow fail-over LKC-1175: Test: Spring WebFlow fail-over
 * https://jira.terracotta.lan/jira/browse/LKC-1175 Test startup Test fail-over More?? Spring Webflow flows are stateful
 * (by nature), they are backed up by the HttpContext so it will be a no brainer to support. Need to work out config, if
 * it is needed and if so what it should be. 1. ContinuationFlowExecutionRepositoryFactory 2.
 * DefaultFlowExecutionRepositoryFactory 3. SingleKeyFlowExecutionRepositoryFactory (extends 2)
 */
public class WebFlowTestBase extends SpringDeploymentTest {
  private static final String  CONTEXT   = "webflow";
  private static final String  TC_CONFIG = "/tc-config-files/webflow-tc-config.xml";

  private WebApplicationServer server0;
  private WebApplicationServer server1;
  private WebApplicationServer server2;
  private WebApplicationServer server3;

  public WebFlowTestBase() {
    //
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    stopAllWebServers();
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");

    builder
        .addDirectoryOrJARContainingClass(org.apache.taglibs.standard.Version.class)
        // standard-1.0.6.jar
        .addDirectoryOrJARContainingClass(javax.servlet.jsp.jstl.core.Config.class)
        // spring-webflow-1.0-rc3.jar
        .addDirectoryOrJARContainingClass(
                                          org.springframework.webflow.engine.builder.xml.XmlFlowRegistryFactoryBean.class)
        // spring-webflow-1.0-rc4.jar
        .addDirectoryOrJARContainingClass(org.springframework.binding.convert.Converter.class)
        // spring-binding-1.0-rc3.jar
        .addDirectoryOrJARContainingClass(org.apache.commons.codec.StringDecoder.class)
        // commons-codec-1.3.jar
        .addDirectoryOrJARContainingClass(ognl.Ognl.class)
        // ognl-2.7.jar
        .addDirectoryOrJARContainingClass(EDU.oswego.cs.dl.util.concurrent.ReentrantLock.class)
        // concurrent-1.3.4.jar for SWF on jdk1.4

        .addResource("/web-resources", "webflow.jsp", "WEB-INF").addResource("/com/tctest/spring", "webflow.xml",
                                                                             "WEB-INF")
        .addResource("/com/tctest/spring", "webflow-beans.xml", "WEB-INF")
        .addResource("/com/tctest/spring", "webflow-servlet.xml", "WEB-INF").addResource("/web-resources",
                                                                                         "weblogic.xml", "WEB-INF")

        .addServlet("webflow", "*.htm", org.springframework.web.servlet.DispatcherServlet.class,
                    Collections.singletonMap("contextConfigLocation", "/WEB-INF/webflow-servlet.xml"), true);

    builder.addDirectoryContainingResource(TC_CONFIG);
    return builder.makeDeployment();
  }

  private WebApplicationServer createServer(Deployment deployment) throws Exception {
    WebApplicationServer server = makeWebApplicationServer(TC_CONFIG);
    server.addWarDeployment(deployment, CONTEXT);
    return server;
  }

  protected void checkWebFlow(String controller, boolean withStepBack) throws Exception {
    Deployment deployment = makeDeployment();
    server0 = createServer(deployment);
    server1 = createServer(deployment);
    server2 = createServer(deployment);
    server3 = createServer(deployment);

    server0.start();

    WebConversation webConversation1 = new WebConversation();

    Response response1 = request(server0, webConversation1, null, null, controller);
    assertTrue("Expecting non-empty flow execution key; " + response1, response1.flowExecutionKey.length() > 0);
    assertEquals("", response1.result);

    server1.start();

    Response response2 = request(server1, webConversation1, response1.flowExecutionKey, "valueA", controller);
    assertTrue("Expecting non-empty flow execution key; " + response2, response2.flowExecutionKey.length() > 0);
    assertEquals("Invalid state; " + response2, WebFlowBean.STATEB, response2.result);
    assertEquals("valueA", response2.valueA);

    Response response3 = request(server0, webConversation1, response2.flowExecutionKey, "valueB", controller);
    assertTrue("Expecting non-empty flow execution key; " + response3, response3.flowExecutionKey.length() > 0);
    assertEquals("Invalid state; " + response3, WebFlowBean.STATEC, response3.result);
    assertEquals("Invalid value; " + response3, "valueB", response3.valueB);

    server0.stop();
    server1.stop(); // both servers are down

    server2.start();

    if (withStepBack) {
      // step back
      Response response3a = request(server2, webConversation1, response2.flowExecutionKey, "valueB1", controller);
      assertTrue("Expecting non-empty flow execution key; " + response3a, response3a.flowExecutionKey.length() > 0);
      assertEquals("Invalid state; " + response3a, WebFlowBean.STATEC, response3a.result);
      assertEquals("Invalid value; " + response3a, "valueB1", response3a.valueB);
    }

    // throw away the step back and continue
    Response response4 = request(server2, webConversation1, response3.flowExecutionKey, "valueC", controller);
    assertTrue("Expecting non-empty flow execution key; " + response4, response4.flowExecutionKey.length() > 0);
    assertEquals("Invalid state; " + response4, WebFlowBean.STATED, response4.result);
    assertEquals("Invalid value; " + response4, "valueC", response4.valueC);

    server3.start();

    Response response5 = request(server3, webConversation1, response4.flowExecutionKey, "valueD", controller);
    // flowExecutionKey is empty since flow is completed
    assertEquals("Invalid state; " + response5, WebFlowBean.COMPLETE, response5.result);
    assertEquals("Invalid value; " + response5, "valueD", response5.valueD);

    server2.stop();
    server3.stop();
  }

  private Response request(WebApplicationServer server, WebConversation conversation, String flowExecutionKey,
                           String value, String controllerName) throws Exception {
    String params = "_eventId=submit";
    if (value != null) {
      params += "&value=" + value;
    }
    if (flowExecutionKey != null) {
      params += "&_flowExecutionKey=" + flowExecutionKey.trim();
    } else {
      params += "&_flowId=webflow";
    }

    WebResponse response = server.ping("/webflow/" + controllerName + "?" + params, conversation);
    return new Response(response.getText().trim());
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
      assertTrue("Expected at least two parameters; " + text, values.length > 1);

      this.result = values[0];
      this.flowExecutionKey = values[1];
      this.valueA = values.length > 2 ? values[2] : null;
      this.valueB = values.length > 3 ? values[3] : null;
      this.valueC = values.length > 4 ? values[4] : null;
      this.valueD = values.length > 5 ? values[5] : null;
    }

    public String toString() {
      return text;
    }

  }

}
