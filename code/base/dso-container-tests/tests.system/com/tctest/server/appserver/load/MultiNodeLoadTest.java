/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import com.tc.test.server.appserver.load.Node;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tctest.webapp.servlets.CounterServlet;

import java.net.URL;
import java.util.ArrayList;


public class MultiNodeLoadTest extends AbstractAppServerTestCase {

  private static final int  SESSIONS_PER_NODE = 10;
  private static final long TEST_DURATION     = 4 * 60 * 1000;
  // private static final long TEST_DURATION = 10 * 1000;

  public MultiNodeLoadTest() {
    // this.disableAllUntil("2006-10-24");
    registerServlet(CounterServlet.class);

    ArrayList args = new ArrayList();
    args.add("-XX:+HeapDumpOnOutOfMemoryError");
    addDsoServerJvmArgs(args);
  }

  public void testFourNodeLoad() throws Throwable {
    runFourNodeLoad(true);
  }

  protected void runFourNodeLoad(boolean sticky) throws Throwable {
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
}
