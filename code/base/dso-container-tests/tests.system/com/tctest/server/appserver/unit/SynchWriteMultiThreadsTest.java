/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import org.apache.commons.httpclient.HttpClient;

import com.tc.test.server.appserver.unit.AbstractAppServerTestCase;
import com.tc.test.server.util.HttpUtil;
import com.tc.util.Assert;
import com.tctest.webapp.servlets.SynchWriteMultiThreadsTestServlet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 */
public class SynchWriteMultiThreadsTest extends AbstractAppServerTestCase {

  private static final int INTENSITY      = 1000;
  private static final int NUM_OF_DRIVERS = 15;

  public SynchWriteMultiThreadsTest() {
    registerServlet(SynchWriteMultiThreadsTestServlet.class);
  }

  private static class Driver extends Thread {
    private HttpClient                 client;
    private int                        port0, port1;
    private SynchWriteMultiThreadsTest parent;
    private List                       errors;

    public Driver(SynchWriteMultiThreadsTest parent, int port0, int port1, List errors) {
      client = HttpUtil.createHttpClient();
      this.parent = parent;
      this.port0 = port0;
      this.port1 = port1;
      this.errors = errors;
    }

    public void run() {
      try {
        URL url0 = parent.createUrl(port0, "server=0&command=ping");
        assertEquals("Send ping", "OK", hit(url0, client));
        URL url1 = parent.createUrl(port1, "server=1&command=ping");
        assertEquals("Receive pong", "OK", hit(url1, client));
        generateTransactionRequests();
      } catch (Throwable e) {
        errors.add(e);
        throw new RuntimeException(e);
      }
    }

    private void generateTransactionRequests() throws Exception {
      for (int i = 0; i < INTENSITY; i++) {
        URL url0 = parent.createUrl(port0, "server=0&command=insert&data=" + i);
        assertEquals("Send data", "OK", hit(url0, client));
      }
    }

    public void validate() throws Exception {
      URL url1 = parent.createUrl(port1, "server=1&command=query&data=0");
      assertEquals("Query 0", "0", hit(url1, client));
      url1 = parent.createUrl(port1, "server=1&command=query&data=" + (INTENSITY - 1));
      assertEquals("Query last attr", "" + (INTENSITY - 1), hit(url1, client));
    }
  }

  public URL createUrl(int port, String query) throws MalformedURLException {
    return super.createUrl(port, SynchWriteMultiThreadsTestServlet.class, query);
  }

  private static String hit(URL url, HttpClient client) throws Exception {
    String response = "";
    try {
      response = HttpUtil.getResponseBody(url, client);
    } catch (Throwable e) {
      Thread.sleep(2000);
      response = HttpUtil.getResponseBody(url, client);
    }

    return response;
  }

  public final void testSessions() throws Exception {

    this.setSynchronousWrite(true);
    this.startDsoServer();

    int port0 = startAppServer(true).serverPort();
    int port1 = startAppServer(true).serverPort();

    List errors = Collections.synchronizedList(new ArrayList());
    // start all drivers
    Driver[] drivers = new Driver[NUM_OF_DRIVERS];
    for (int i = 0; i < NUM_OF_DRIVERS; i++) {
      drivers[i] = new Driver(this, port0, port1, errors);
      drivers[i].start();
    }

    // wait for all of them to finish
    for (int i = 0; i < NUM_OF_DRIVERS; i++) {
      drivers[i].join();
    }

    // proceed only if there are no errors inside any threads
    if (errors.size() == 0) {
      // send kill signal to server0
      killServer0(port0);

      // validate data on server1
      for (int i = 0; i < NUM_OF_DRIVERS; i++) {
        drivers[i].validate();
      }
    } else {
      Assert.failure("Exception found in driver thread. ", (Throwable) errors.get(0));
    }
  }

  private void killServer0(int port0) {
    try {
      URL url0 = createUrl(port0, "server=0&command=kill");
      assertEquals("OK", hit(url0, HttpUtil.createHttpClient()));
    } catch (Throwable e) {
      // expected
      System.err.println("Caught exception from kill server0");
    }
  }
}
