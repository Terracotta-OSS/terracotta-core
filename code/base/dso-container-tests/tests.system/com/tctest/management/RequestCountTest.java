/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.management;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.test.server.appserver.load.Node;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class RequestCountTest extends AbstractAppServerTestCase {

  private static final TCLogger logger             = TCLogging.getTestingLogger(RequestCountTest.class);

  private static final int      SESSIONS_PER_NODE  = 10;
  private static final long     TEST_DURATION      = 30 * 1000;
  private static final String   CLIENT_NAME_PREFIX = "client-";

  public void testRequestCount() throws Throwable {
    collectVmStats();
    List jvmArgs = new ArrayList(1);
    jvmArgs.add("-Dcom.sun.management.jmxremote");
    addDsoServerJvmArgs(jvmArgs);
    startDsoServer();
    runNodes(2);
  }

  private void runNodes(int nodeCount) throws Throwable {
    Thread[] nodeRunners = new Thread[nodeCount];
    NodeWithJMX[] nodes = new NodeWithJMX[nodeCount];
    int[] ports = new int[nodeCount];

    for (int i = 0; i < nodeCount; i++) {
      ports[i] = startAppServer(true, new Properties(), new String[] { "-Dtc.node-name=" + CLIENT_NAME_PREFIX + i })
          .serverPort();
    }

    final JMXConnector jmxConnector = getJMXConnector();
    final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    final long beanCutoffTime = System.currentTimeMillis() + (60 * 1000);
    Set beanSet = null;
    for (beanSet = TerracottaManagement.getAllSessionMonitorMBeans(mbs); beanSet.size() != nodeCount; beanSet = TerracottaManagement
        .getAllSessionMonitorMBeans(mbs)) {
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
      logger.info("\t" + ((ObjectName) pos.next()).getCanonicalName());
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
      URL mutateUrl = createUrl(ports[i], RequestCountingServlet.class);
      URL validateUrl = createUrl(ports[(i + 1) % nodeCount], RequestCountingServlet.class, "read=true");
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

  private JMXConnector getJMXConnector() throws IOException {
    JMXServiceURL jmxServerUrl = new JMXServiceURL("jmxmp", "localhost", getJMXPort());
    JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(jmxServerUrl, null);
    jmxConnector.connect();
    return jmxConnector;
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
      System.err.println("validated value of " + totalRequests + " for client " + validateUrl);
    }
  }

  public static final class RequestCountingServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();

      Integer count = (Integer) session.getAttribute("count");
      if (count == null) {
        count = new Integer(0);
      }

      if (request.getParameter("read") != null) {
        if (session.isNew()) {
          out.println("session is new"); // this is an error condition (client will fail trying to parse this as int)
        } else {
          out.println(count.intValue());
        }
      } else {
        int newValue = count.intValue() + 1;
        session.setAttribute("count", new Integer(newValue));
        out.println(newValue);
      }
    }
  }
}