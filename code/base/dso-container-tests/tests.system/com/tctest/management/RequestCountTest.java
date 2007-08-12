/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.management;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.appserver.load.Node;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.CounterServlet;

import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import junit.framework.Test;

public final class RequestCountTest extends AbstractDeploymentTest {
  private static final TCLogger logger             = TCLogging.getTestingLogger(RequestCountTest.class);

  private static final int      SESSIONS_PER_NODE  = 10;
  private static final long     TEST_DURATION      = 30 * 1000;
  private static final String   CLIENT_NAME_PREFIX = "client-";

  private static final String   CONTEXT            = "RequestCountTest";
  private static final String   SERVLET            = "RequestCountingServlet";

  private Deployment            deployment;
  private TcConfigBuilder       configBuilder;

  public static Test suite() {
    return new ServerTestSetup(RequestCountTest.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    if (deployment == null) {
      deployment = makeDeployment();
      configBuilder = new TcConfigBuilder();
      configBuilder.addWebApplication(CONTEXT);
    }
  }

  public void testRequestCount() throws Throwable {
    assertTimeDirection();
    runNodes(2);
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    builder.addServlet(SERVLET, "/" + SERVLET + "/*", CounterServlet.class, null, false);
    return builder.makeDeployment();
  }

  private int createAndStartServer(String extraJvmArg) throws Exception {
    WebApplicationServer server = makeWebApplicationServer(configBuilder);
    server.addWarDeployment(deployment, CONTEXT);
    server.getServerParameters().appendJvmArgs(extraJvmArg);
    server.start();
    return server.getPort();
  }

  private URL createUrl(int port, String params) throws Exception {
    URL url = new URL("http://localhost:" + port + "/" + CONTEXT + "/" + SERVLET
                      + (params.length() > 0 ? "?" + params : ""));
    return url;
  }

  private void runNodes(int nodeCount) throws Throwable {
    Thread[] nodeRunners = new Thread[nodeCount];
    NodeWithJMX[] nodes = new NodeWithJMX[nodeCount];
    int[] ports = new int[nodeCount];

    for (int i = 0; i < nodeCount; i++) {
      ports[i] = createAndStartServer("-Dtc.node-name=" + CLIENT_NAME_PREFIX + i);
    }

    final JMXConnector jmxConnector = getJMXConnector();
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    final long beanCutoffTime = System.currentTimeMillis() + (60 * 1000);
    Set beanSet = null;
    for (beanSet = TerracottaManagement.getAllSessionMonitorMBeans(mbs); beanSet.size() != nodeCount; beanSet = TerracottaManagement
        .getAllSessionMonitorMBeans(mbs)) {
      if (!beanSet.isEmpty()) {
        logger.info("Found some session beans but not all:");
        for (final Iterator pos = beanSet.iterator(); pos.hasNext();) {
          logger.info("Session bean[" + ((ObjectName) pos.next()).getCanonicalName() + "]");
        }
      } else {
        logger.info("No session beans found, expecting " + nodeCount + " of them");
      }
      if (System.currentTimeMillis() > beanCutoffTime) {
        final String errMsg = "Unable to find DSO client MBeans within 60 seconds of starting the node";
        logger.error(errMsg);
        fail(errMsg);
      } else {
        logger.info("Unable to find DSO client MBeans after " + (beanCutoffTime - System.currentTimeMillis())
                    + "ms, sleeping for 3 seconds");
        try {
          Thread.sleep(3000);
        } catch (InterruptedException ie) {
          // ignore
        }
      }
    }
    logger.info("We have all of our DSO client Mbeans after " + (beanCutoffTime - System.currentTimeMillis()) + ":");
    for (final Iterator pos = beanSet.iterator(); pos.hasNext();) {
      logger.info("Session bean[" + ((ObjectName) pos.next()).getCanonicalName() + "]");
    }
    final SessionMonitorMBean[] mbeans = new SessionMonitorMBean[nodeCount];
    for (int i = 0; i < nodeCount; i++) {
      SessionMonitorMBean clientMBean = TerracottaManagement.getClientSessionMonitorMBean(mbs, beanSet,
                                                                                          CLIENT_NAME_PREFIX + i);
      if (clientMBean == null) {
        final String errMsg = "We thought we had all of our DSO client MBeans, turns out we don't have one for client["
                              + CLIENT_NAME_PREFIX + i + "]";
        logger.error(errMsg);
        fail(errMsg);
      }
      mbeans[i] = clientMBean;
    }

    for (int i = 0; i < nodeCount; i++) {
      URL mutateUrl = createUrl(ports[i], "");
      URL validateUrl = createUrl(ports[(i + 1) % nodeCount], "read=true");
      nodes[i] = new NodeWithJMX(mutateUrl, validateUrl, SESSIONS_PER_NODE, TEST_DURATION);
      nodeRunners[i] = new Thread(nodes[i], "Runner for server at port " + ports[i]);
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

    for (int i = 0; i < nodeCount; i++) {
      nodes[i].validateMBean(mbeans[i]);
    }

    jmxConnector.close();
  }

  private JMXConnector getJMXConnector() {
    int jmxPort = getServerManager().getServerTcConfig().getJmxPort();
    return new JMXConnectorProxy("localhost", jmxPort);
  }

  private static class NodeWithJMX extends Node {

    public NodeWithJMX(URL mutateUrl, URL validateUrl, int numSessions, long duration) {
      super(mutateUrl, validateUrl, numSessions, duration);
    }

    public void validateMBean(SessionMonitorMBean sessionMonitorMBean) {
      int totalRequests = 0;
      for (int i = 0; i < numRequests.length; i++) {
        totalRequests += numRequests[i];
      }

      assertEquals(totalRequests, sessionMonitorMBean.getRequestCount());

      assertEquals(1, validateUrls.length);
      final URL validateUrl = validateUrls[0];

      System.err.println("validated value of " + totalRequests + " for client " + validateUrl);
    }
  }
}
