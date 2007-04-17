/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.dso;

import org.apache.commons.httpclient.HttpClient;

import com.tc.object.config.schema.AutoLock;
import com.tc.object.config.schema.LockLevel;
import com.tc.object.config.schema.Root;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DsoRootTest extends AbstractAppServerTestCase {
  private static final int TOTAL_REQUEST_COUNT = 100;

  protected boolean isSessionTest() {
    return false;
  }

  public void testRoot() throws Throwable {
    List roots = new ArrayList();
    String rootName = "counterObject";
    String fieldName = RootCounterServlet.class.getName() + ".counterObject";
    roots.add(new Root(rootName, fieldName));
    addRoots(roots);

    List locks = new ArrayList();
    LockLevel lockLevel = LockLevel.WRITE;
    String methodExpression = "* " + RootCounterServlet.class.getName() + "$Counter.*(..)";
    locks.add(new AutoLock(methodExpression, lockLevel));
    addLocks(locks);

    startDsoServer();
    runNodes(2);
  }

  private void runNodes(int nodeCount) throws Throwable {
    HttpClient client = HttpUtil.createHttpClient();

    int[] ports = new int[nodeCount];
    URL[] urls = new URL[nodeCount];

    for (int i = 0; i < nodeCount; i++) {
      ports[i] = startAppServer(true).serverPort();
      urls[i] = createUrl(ports[i], RootCounterServlet.class);
    }

    Random random = new Random();
    for (int i = 0, currentRequestCount = 0; i < TOTAL_REQUEST_COUNT && currentRequestCount < TOTAL_REQUEST_COUNT; i++) {
      int remainingRequests = TOTAL_REQUEST_COUNT - currentRequestCount;
      for (int j = 0; j < random.nextInt(remainingRequests + 1); j++) {
        int newVal = HttpUtil.getInt(urls[i % nodeCount], client);
        currentRequestCount++;
        assertEquals(currentRequestCount, newVal);
      }
    }
  }

  public static class RootCounterServlet extends HttpServlet {
    private final Counter counterObject = new Counter();

    private static class Counter {
      private int counter;

      public Counter() {
        counter = 0;
      }

      public synchronized void increment() {
        counter++;
      }

      public synchronized void setValue(int newValue) {
        counter = newValue;
      }

      public synchronized int getValue() {
        return counter;
      }
    }

    private int getCurrentCountValue() {
      counterObject.increment();
      return counterObject.getValue();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println(getCurrentCountValue());
    }
  }
}
