/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.appserver.load.Node;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.CounterServlet;

import java.net.URL;

import junit.framework.Test;

public class MultiNodeLoadTest extends AbstractDeploymentTest {
  private static final String CONTEXT           = "MultiNodeLoadTest";
  private static final String SERVLET           = "CounterServlet";

  private static final int    SESSIONS_PER_NODE = 10;
  private static final long   TEST_DURATION     = 4 * 60 * 1000;

  private Deployment          deployment;
  private TcConfigBuilder     configBuilder;

  public static Test suite() {
    return new ServerTestSetup(MultiNodeLoadTest.class);
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    builder.addServlet(SERVLET, "/" + SERVLET + "/*", CounterServlet.class, null, false);
    return builder.makeDeployment();
  }

  public void setUp() throws Exception {
    super.setUp();
    if (deployment == null) {
      deployment = makeDeployment();
      configBuilder = new TcConfigBuilder();
      configBuilder.addWebApplication(CONTEXT);
    }
  }

  public void testFourNodeLoad() throws Throwable {
    runFourNodeLoad(true);
  }

  protected void runFourNodeLoad(boolean sticky) throws Throwable {
    assertTimeDirection();
    runNodes(4, sticky);
  }

  private WebApplicationServer createAndStartServer() throws Exception {
    WebApplicationServer server = makeWebApplicationServer(configBuilder);
    server.addWarDeployment(deployment, CONTEXT);
    server.start();
    return server;
  }

  private URL createUrl(int port, String params) throws Exception {
    URL url = new URL("http://localhost:" + port + "/" + CONTEXT + "/" + SERVLET
                      + (params.length() > 0 ? "?" + params : ""));
    return url;
  }

  private void runNodes(int nodeCount, boolean sticky) throws Throwable {
    Thread[] nodeRunners = new Thread[nodeCount];
    Node[] nodes = new Node[nodeCount];
    WebApplicationServer[] servers = new WebApplicationServer[nodeCount];

    URL[] allUrls = new URL[nodeCount];
    for (int i = 0; i < nodeCount; i++) {
      servers[i] = createAndStartServer();
      allUrls[i] = createUrl(servers[i].getPort(), "");
    }

    URL[] validateUrls = new URL[nodeCount];
    for (int i = 0; i < nodeCount; i++) {
      validateUrls[i] = createUrl(servers[i].getPort(), "read=true");
    }

    for (int i = 0; i < nodeCount; i++) {
      if (sticky) {
        URL validateUrl = createUrl(servers[(i + 1) % nodeCount].getPort(), "read=true");
        nodes[i] = new Node(allUrls[i], validateUrl, SESSIONS_PER_NODE, TEST_DURATION);
      } else {
        nodes[i] = new Node(allUrls, validateUrls, SESSIONS_PER_NODE, TEST_DURATION);
      }

      nodeRunners[i] = new Thread(nodes[i], "Runner[" + servers[i].getPort() + "]");
    }

    for (int i = 0; i < nodeCount; i++) {
      nodeRunners[i].start();
    }

    for (int i = 0; i < nodeCount; i++) {
      nodeRunners[i].join();
    }

    for (int i = 0; i < nodeCount; i++) {
      nodes[i].checkError();
    }
  }
}
