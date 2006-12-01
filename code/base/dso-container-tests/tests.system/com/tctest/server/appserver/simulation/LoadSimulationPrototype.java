/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.server.appserver.simulation;

import org.apache.commons.httpclient.HttpClient;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.test.server.appserver.AppServerResult;
import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class LoadSimulationPrototype extends AbstractAppServerTestCase {

  private static final int                   STICKY             = 70;
  private static final int                   REQUEST_ITERATIONS = 1000;
  private static final SynchronizedBoolean[] stickyResults      = new SynchronizedBoolean[REQUEST_ITERATIONS];
  private final PooledExecutor               pool               = new PooledExecutor(5);
  private final AppServerResult[]            containers         = new AppServerResult[2];
  private final SessionWrapper[]             sessions           = new SessionWrapper[100];
  private final SynchronizedInt              done               = new SynchronizedInt(0);
  private final List                         errors             = new ArrayList();

  public void setUp() throws Exception {
    super.setUp();
    for (int i = 0; i < sessions.length; i++) {
      sessions[i] = new SessionWrapper(HttpUtil.createHttpClient());
    }

    for (int i = 0; i < stickyResults.length; i++) {
      stickyResults[i] = new SynchronizedBoolean(false);
    }

  }

  public void testSessions() throws Exception {

    startDsoServer();

    for (int i = 0; i < containers.length; i++) {
      containers[i] = startAppServer(true);
    }

    pool.setKeepAliveTime(1000);

    for (int i = 0; i < REQUEST_ITERATIONS; i++) {
      pool.execute(new Requestor(i));
    }

    pool.shutdownAfterProcessingCurrentlyQueuedTasks();
    pool.awaitTerminationAfterShutdown();

    // This is just a sanity check
    assertEquals(REQUEST_ITERATIONS, done.get());

    synchronized (errors) {
      for (Iterator i = errors.iterator(); i.hasNext();) {
        Throwable t = (Throwable) i.next();
        t.printStackTrace();
      }
      if (errors.size() > 0) {
        fail("requests encountered errors");
      }
    }

    tallyResults();
  }

  private void reportError(Throwable t) {
    synchronized (errors) {
      errors.add(t);
    }
  }

  private void tallyResults() {
    int stickyCount = 0;
    for (int i = 0; i < stickyResults.length; i++) {
      if (stickyResults[i].get()) stickyCount++;
    }

    double result = ((double) stickyCount / (double) stickyResults.length) * 100;

    System.out.println("ORIGINAL STICKYNESS   = " + STICKY + "%");
    System.out.println("ACTUAL STICKYNESS     = " + result + "%");

    assertTrue(true);
  }

  private int getRandom(int seed) {
    return new Long(Math.round(Math.floor(seed * Math.random()))).intValue();
  }

  public static final class StickySessionServlet extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(true);
      String instance = System.getProperty("app_instance");
      String session_instance = (String) session.getAttribute("instance");

      boolean isSticky = true;
      if (session_instance != null && !session_instance.equals(instance)) isSticky = false;
      session.setAttribute("instance", instance);

      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      out.println(isSticky);
      System.out.println(session.getId() + ": " + isSticky);
    }
  }

  private class Requestor implements Runnable {

    private final int iterationIndex;

    private Requestor(int iteration) {
      iterationIndex = iteration;
    }

    public void run() {
      try {
        int randomSession = getRandom(sessions.length);
        int randomValue = getRandom(100);
        if (!(STICKY >= randomValue + 1)) {
          int prevPort = sessions[randomSession].getContainerPort();
          int newPort;
          do {
            newPort = containers[getRandom(containers.length)].serverPort();
            sessions[randomSession].setContainerPort(newPort);
          } while (newPort == prevPort);
        }

        URL url = createUrl(sessions[randomSession].getContainerPort(),
                            LoadSimulationPrototype.StickySessionServlet.class);
        stickyResults[iterationIndex].set(HttpUtil.getBoolean(url, sessions[randomSession].getSession()));
      } catch (Throwable t) {
        reportError(t);
      } finally {
        done.increment();
      }
    }
  }

  private class SessionWrapper {

    private HttpClient session;
    private int        containerPort = -1;

    private SessionWrapper(HttpClient session) {
      this.session = session;
    }

    private synchronized HttpClient getSession() {
      return session;
    }

    private synchronized void setContainerPort(int containerPort) {
      this.containerPort = containerPort;
    }

    private synchronized int getContainerPort() {
      if (containerPort == -1) {
        containerPort = containers[getRandom(containers.length)].serverPort();
      }
      return containerPort;
    }
  }
}
