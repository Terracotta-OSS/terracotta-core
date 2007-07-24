/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.SynchWriteMultiThreadsTestServlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

/**
 * Test session synchronous write with heavy load
 */
public class SynchWriteMultiThreadsTest extends AbstractTwoServerDeploymentTest {
  private static final String CONTEXT        = "SynchWriteMultiThreadsTest";
  private static final String SERVLET        = "SynchWriteMultiThreadsTestServlet";
  private static final int    INTENSITY      = 1000;
  private static final int    NUM_OF_DRIVERS = 15;

  public static Test suite() {
    return new SynchWriteMultiThreadsTestSetup();
  }

  private static class Driver extends Thread {
    private WebConversation            wc;
    private SynchWriteMultiThreadsTest parent;
    private List                       errors;

    public Driver(SynchWriteMultiThreadsTest parent, List errors, WebConversation wc) {
      this.parent = parent;
      this.errors = errors;
      this.wc = wc;
    }

    public void run() {
      try {
        assertEquals("OK", request(parent.server0, "server=0&command=ping", wc));
        assertEquals("OK", request(parent.server1, "server=1&command=ping", wc));
        generateTransactionRequests();
      } catch (Throwable e) {
        errors.add(e);
        throw new RuntimeException(e);
      }
    }

    private void generateTransactionRequests() throws Exception {
      for (int i = 0; i < INTENSITY; i++) {
        assertEquals("OK", request(parent.server0, "server=0&command=insert&data=" + i, wc));
      }
    }

    public void validate() throws Exception {
      assertEquals("0", request(parent.server1, "server=1&command=query&data=0", wc));
      assertEquals("" + (INTENSITY - 1), request(parent.server1, "server=1&command=query&data=" + (INTENSITY - 1), wc));
    }
  }

  private static String request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + SERVLET + "?" + params, con).getText().trim();
  }

  public final void testSynchWriteWithLoad() throws Exception {
    WebConversation wc = new WebConversation();

    List errors = Collections.synchronizedList(new ArrayList());
    // start all drivers
    Driver[] drivers = new Driver[NUM_OF_DRIVERS];
    for (int i = 0; i < NUM_OF_DRIVERS; i++) {
      drivers[i] = new Driver(this, errors, wc);
      drivers[i].start();
    }

    // wait for all of them to finish
    for (int i = 0; i < NUM_OF_DRIVERS; i++) {
      drivers[i].join();
    }

    // proceed only if there are no errors inside any threads
    if (errors.size() == 0) {
      // send kill signal to server0
      killServer0(server0);

      // validate data on server1
      for (int i = 0; i < NUM_OF_DRIVERS; i++) {
        drivers[i].validate();
      }
    } else {
      fail("Exception found in driver thread. ", (Throwable) errors.get(0));
    }
  }

  private void killServer0(WebApplicationServer server) {
    try {
      assertEquals("OK", request(server, "server=0&command=kill", new WebConversation()));
    } catch (Throwable e) {
      // expected
      System.err.println("Caught expected exception from killing server1");
    }
  }

  private static class SynchWriteMultiThreadsTestSetup extends TwoServerTestSetup {
    public SynchWriteMultiThreadsTestSetup() {
      super(SynchWriteMultiThreadsTest.class, CONTEXT);
    }

    protected void configureWar(DeploymentBuilder builder) {
      builder.addServlet(SERVLET, "/" + SERVLET + "/*", SynchWriteMultiThreadsTestServlet.class, null, false);
    }

    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addWebApplication(CONTEXT, true);
    }
  }
}
