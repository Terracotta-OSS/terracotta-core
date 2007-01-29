/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import com.tc.test.server.appserver.load.Node;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class MultiNodeLoadTest extends AbstractAppServerTestCase {
  private static final int  SESSIONS_PER_NODE = 10;

  private static final long TEST_DURATION     = 4 * 60 * 1000;

  public MultiNodeLoadTest() {
    // this.disableAllUntil("2006-10-24");
  }

  public void testFourNodeLoad() throws Throwable {
    runFourNodeLoad(true);
  }

  protected void runFourNodeLoad(boolean sticky) throws Throwable {
    collectVmStats();
    assertTimeDirection();
    startDsoServer();
    runNodes(4, sticky);
  }

  private void runNodes(int nodeCount, boolean sticky) throws Throwable {
    Thread[] nodeRunners = new Thread[nodeCount];
    Node[] nodes = new Node[nodeCount];
    int[] ports = new int[nodeCount];

    URL[] allUrls = new URL[nodeCount];
    for (int i = 0; i < nodeCount; i++) {
      ports[i] = startAppServer(true).serverPort();
      allUrls[i] = createUrl(ports[i], CounterServlet.class);
    }

    URL[] validateUrls = new URL[nodeCount];
    for (int i = 0; i < nodeCount; i++) {
      validateUrls[i] = createUrl(ports[i], CounterServlet.class, "read=true");
    }

    for (int i = 0; i < nodeCount; i++) {
      if (sticky) {
        URL validateUrl = createUrl(ports[(i + 1) % nodeCount], CounterServlet.class, "read=true");
        nodes[i] = new Node(allUrls[i], validateUrl, SESSIONS_PER_NODE, TEST_DURATION);
      } else {
        nodes[i] = new Node(allUrls, validateUrls, SESSIONS_PER_NODE, TEST_DURATION);
      }

      nodeRunners[i] = new Thread(nodes[i], "Runner[" + ports[i] + "]");
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

  public static class CounterServlet extends HttpServlet {

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
