/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.dso;

import org.apache.commons.httpclient.HttpClient;

import com.tc.object.config.schema.AutoLock;
import com.tc.object.config.schema.LockLevel;
import com.tc.object.config.schema.Root;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tctest.webapp.servlets.RootCounterServlet;

import java.net.URL;
import java.util.Random;

public class DsoRootTest extends AbstractAppServerTestCase {

  private static final int TOTAL_REQUEST_COUNT = 100;

  public DsoRootTest() {
    //DEV-797
    disableAllUntil("2007-08-01");
    registerServlet(RootCounterServlet.class);
  }

  protected boolean isSessionTest() {
    return false;
  }

  public void testRoot() throws Throwable {
    String rootName = "counterObject";
    String fieldName = RootCounterServlet.class.getName() + ".counterObject";
    addRoot(new Root(rootName, fieldName));

    LockLevel lockLevel = LockLevel.WRITE;
    String methodExpression = "* " + RootCounterServlet.class.getName() + "$Counter.*(..)";
    addLock(new AutoLock(methodExpression, lockLevel));

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

}
