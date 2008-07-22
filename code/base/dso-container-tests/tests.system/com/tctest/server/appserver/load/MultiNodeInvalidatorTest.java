/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.load;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.Deployment;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.listeners.MultiNodeInvalidatorListener;
import com.tctest.webapp.servlets.MultiNodeInvalidatorSerlvet;

import java.util.Random;

import junit.framework.Test;

public class MultiNodeInvalidatorTest extends AbstractDeploymentTest {
  private static final String CONTEXT                = "MultiNodeInvalidatorTest";
  private static final String SERVLET                = "MultiNodeInvalidatorServlet";

  private static final int    SESSIONS_PER_THREAD    = 500;
  private static final int    NUM_LOAD_THREADS       = 4;

  private static final long   LOAD_DURATION          = 3 * 60 * 1000;

  private static final int    ABANDON_RATE           = 25;
  private static final int    IDLE_SECONDS           = 10;
  private static final int    INVALIDATOR_SLEEP_SECS = 5;

  private static final String LOAD_REQUEST           = "/" + CONTEXT + "/" + SERVLET + "?idle=" + IDLE_SECONDS;
  private static final String QUERY_REQUEST          = "/" + CONTEXT + "/" + SERVLET + "?query=query";

  private Deployment          deployment;
  private TcConfigBuilder     configBuilder;

  public static Test suite() {
    return new ServerTestSetup(MultiNodeInvalidatorTest.class);
  }

  public MultiNodeInvalidatorTest() {
    //
  }

  private Deployment makeDeployment() throws Exception {
    DeploymentBuilder builder = makeDeploymentBuilder(CONTEXT + ".war");
    builder.addListener(MultiNodeInvalidatorListener.class);
    builder.addServlet(SERVLET, "/" + SERVLET + "/*", MultiNodeInvalidatorSerlvet.class, null, false);
    return builder.makeDeployment();
  }

  public void setUp() throws Exception {
    super.setUp();
    if (deployment == null) {
      deployment = makeDeployment();
      configBuilder = new TcConfigBuilder();
      configBuilder.addRoot(MultiNodeInvalidatorListener.class.getName() + ".sessionIDs", "sessionIDs");
      configBuilder.addAutoLock("* " + MultiNodeInvalidatorListener.class.getName() + ".*(..)", "write");
      configBuilder.addWebApplication(CONTEXT);
    }
  }

  private WebApplicationServer createAndStartServer() throws Exception {
    WebApplicationServer server = makeWebApplicationServer(configBuilder);
    StandardAppServerParameters params = server.getServerParameters();

    params.appendSysProp(TCPropertiesImpl.SYSTEM_PROP_PREFIX + TCPropertiesConsts.SESSION_INVALIDATOR_SLEEP,
                         INVALIDATOR_SLEEP_SECS);
    params.appendSysProp(TCPropertiesImpl.SYSTEM_PROP_PREFIX + TCPropertiesConsts.SESSION_DEBUG_INVALIDATE, true);

    server.addWarDeployment(deployment, CONTEXT);
    server.start();
    return server;
  }

  public void testLoad() throws Throwable {
    runLoad(LowMemWorkaround.computeNumberOfNodes(4, appServerInfo()));
  }

  private void runLoad(final int numServers) throws Throwable {
    WebApplicationServer[] servers = new WebApplicationServer[numServers];
    for (int i = 0; i < servers.length; i++) {
      servers[i] = createAndStartServer();
    }

    Load[] loadThreads = new Load[NUM_LOAD_THREADS];

    for (int i = 0; i < NUM_LOAD_THREADS; i++) {
      loadThreads[i] = new Load(servers);
      loadThreads[i].start();
    }

    for (int i = 0; i < NUM_LOAD_THREADS; i++) {
      loadThreads[i].finish();
    }

    while (true) {
      int numSessions = Integer.parseInt(servers[0].ping(QUERY_REQUEST).getText().trim());
      if (numSessions == 0) {
        break;
      }
      System.err.println(numSessions + " sessions not yet invalidated, sleeping...");
      ThreadUtil.reallySleep(5000);
    }
  }

  private static class Load extends Thread {

    private final SynchronizedRef        error    = new SynchronizedRef(null);
    private final WebApplicationServer[] servers;
    private final Random                 r        = new Random();
    private final WebConversation[]      sessions = new WebConversation[SESSIONS_PER_THREAD];

    public Load(WebApplicationServer[] servers) {
      this.servers = servers;
    }

    public void run() {
      try {
        run0();
      } catch (Throwable t) {
        error.set(t);
      }
    }

    private void run0() throws Exception {
      init();
      final long end = System.currentTimeMillis() + LOAD_DURATION;
      while (System.currentTimeMillis() < end) {
        request(getRandomServer(), sessions[getRandomSessionIndex()]);
        if (r.nextInt(ABANDON_RATE) == 0) {
          sessions[getRandomSessionIndex()] = newSession();
        }
        ThreadUtil.reallySleep(r.nextInt(5) + 1);
      }
    }

    private void init() throws Exception {
      for (int i = 0; i < sessions.length; i++) {
        sessions[i] = newSession();
      }
    }

    private WebConversation newSession() throws Exception {
      WebConversation wc = new WebConversation();
      request(getRandomServer(), wc);
      return wc;
    }

    private void request(WebApplicationServer server, WebConversation wc) throws Exception {
      WebResponse ping = server.ping(LOAD_REQUEST, wc);
      Integer.parseInt(ping.getText().trim());
    }

    void finish() throws Throwable {
      join();
      Throwable t = (Throwable) error.get();
      if (t != null) { throw t; }
    }

    WebApplicationServer getRandomServer() {
      return servers[r.nextInt(servers.length)];
    }

    int getRandomSessionIndex() {
      return r.nextInt(sessions.length);
    }
  }

}
