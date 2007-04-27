/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.load;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.server.util.WebClient;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.Random;

import junit.framework.Assert;

public class Node implements Runnable {
  protected static final TCLogger logger = TCLogging.getLogger(Node.class);
  protected final WebClient[]     clients;
  protected final long            duration;
  protected final int             numRequests[];
  protected final SynchronizedRef error  = new SynchronizedRef(null);
  protected final URL[]           mutateUrls;
  protected final URL[]           validateUrls;
  protected final Random          random = new Random();

  public Node(URL mutateUrl, URL validateUrl, int numSessions, long duration) {
    this(new URL[] { mutateUrl }, new URL[] { validateUrl }, numSessions, duration);
  }

  public Node(URL[] mutateUrls, URL[] validateUrls, int numSessions, long duration) {
    this.mutateUrls = mutateUrls;
    this.validateUrls = validateUrls;
    clients = createClients(numSessions);
    this.duration = duration;
    this.numRequests = new int[clients.length];
  }

  private WebClient[] createClients(int numSessions) {
    WebClient[] _clients = new WebClient[numSessions];
    for (int i = 0; i < numSessions; i++) {
      _clients[i] = new WebClient();
    }

    return _clients;
  }

  public void checkError() throws Throwable {
    Throwable t = (Throwable) error.get();
    if (t != null) { throw t; }
  }

  public void run() {
    try {
      makeRequests();
      validate();
    } catch (Throwable t) {
      logger.error(t);
      error.set(t);
    }
  }

  private void validate() throws ConnectException, IOException {
    for (int i = 0; i < clients.length; i++) {
      int expect = numRequests[i];
      if (expect == 0) { throw new AssertionError("No requests were ever made for client " + i); }

      WebClient client = clients[i];

      for (int u = 0; u < validateUrls.length; u++) {
        int actual = client.getResponseAsInt(validateUrls[u]);
        Assert.assertEquals(getSessionID(client), expect, actual);
        logger.info("validated value of " + expect + " for client " + i + " on " + validateUrls[u]);
        // Recording the request that was just made. This is needed for RequestCountTest.
        numRequests[i]++;
      }
    }
  }

  private void makeRequests() throws Exception {
    final int numURLS = mutateUrls.length;

    int session = 0;
    final long end = System.currentTimeMillis() + duration;
    while (System.currentTimeMillis() <= end) {
      WebClient client = clients[session];
      URL mutateUrl = numURLS == 1 ? mutateUrls[0] : mutateUrls[random.nextInt(mutateUrls.length)];

      final long start = System.currentTimeMillis();
      try {
        int newVal = client.getResponseAsInt(mutateUrl);
        numRequests[session]++;
        Assert.assertEquals(getSessionID(client), numRequests[session], newVal);
        session = (session + 1) % clients.length;
        ThreadUtil.reallySleep(random.nextInt(5) + 1);
      } catch (Exception e) {
        logger.error("Elapsed time for failed request was " + (System.currentTimeMillis() - start) + " millis, url = "
                     + mutateUrl);
        throw e;
      }
    }
  }

  private String getSessionID(WebClient client) {
    return (String) client.getCookies().get("jsessionid");
  }

}
