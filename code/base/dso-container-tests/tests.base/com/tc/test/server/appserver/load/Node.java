/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.load;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.IntList;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.ThreadDump;

import java.net.URL;
import java.util.Random;

import junit.framework.Assert;

public class Node implements Runnable {
  protected static final TCLogger   logger = TCLogging.getLogger(Node.class);

  protected final long              duration;
  protected final int               numRequests[];
  protected final SynchronizedRef   error  = new SynchronizedRef(null);
  protected final URL[]             mutateUrls;
  protected final URL[]             validateUrls;
  protected final Random            random = new Random();
  protected final WebConversation[] conversations;
  protected final IntList[]         requestTimes;

  public Node(URL mutateUrl, URL validateUrl, int numSessions, long duration) {
    this(new URL[] { mutateUrl }, new URL[] { validateUrl }, numSessions, duration);
  }

  public Node(URL[] mutateUrls, URL[] validateUrls, int numSessions, long duration) {
    this.mutateUrls = mutateUrls;
    this.validateUrls = validateUrls;
    this.conversations = createConversations(numSessions);
    this.duration = duration;
    this.numRequests = new int[numSessions];
    this.requestTimes = new IntList[numSessions];
    for (int i = 0; i < requestTimes.length; i++) {
      requestTimes[i] = new IntList();
    }
  }

  private WebConversation[] createConversations(int count) {
    WebConversation[] wc = new WebConversation[count];
    for (int i = 0; i < count; i++) {
      wc[i] = new WebConversation();
    }
    return wc;
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

  private void validate() throws Exception {
    for (int i = 0; i < conversations.length; i++) {
      int expect = numRequests[i];
      if (expect == 0) {
        if (Os.isUnix() && !Os.isMac()) {
          ThreadDump.dumpProcessGroupMany(3, 500);
        }

        dumpRequestTimes();

        throw new AssertionError("No requests were ever made for client " + i);
      }

      WebConversation wc = conversations[i];

      for (int u = 0; u < validateUrls.length; u++) {
        int actual = getResponseAsInt(wc, validateUrls[u]);
        Assert.assertEquals(getSessionID(wc), expect, actual);
        logger.info("validated value of " + expect + " for client " + i + " on " + validateUrls[u]);
        // Recording the request that was just made. This is needed for RequestCountTest.
        numRequests[i]++;
      }
    }
  }

  private void dumpRequestTimes() {
    for (int i = 0; i < requestTimes.length; i++) {
      logger.info("request times for client " + i + ": " + requestTimes[i]);
    }
  }

  private int getResponseAsInt(WebConversation wc, URL url) throws Exception {
    WebResponse response = wc.getResponse(url.toString());
    return Integer.parseInt(response.getText().trim());
  }

  private void makeRequests() throws Exception {
    final int numURLS = mutateUrls.length;

    int session = 0;
    final long end = System.currentTimeMillis() + duration;
    while (System.currentTimeMillis() <= end) {
      WebConversation wc = conversations[session];
      URL mutateUrl = numURLS == 1 ? mutateUrls[0] : mutateUrls[random.nextInt(mutateUrls.length)];

      final long start = System.currentTimeMillis();
      try {
        int newVal = getResponseAsInt(wc, mutateUrl);
        long elapsed = System.currentTimeMillis() - start;
        requestTimes[session].add((int) elapsed);
        numRequests[session]++;
        Assert.assertEquals(getSessionID(wc), numRequests[session], newVal);
        session = (session + 1) % conversations.length;
        ThreadUtil.reallySleep(random.nextInt(5) + 1);
      } catch (Exception e) {
        logger.error("Elapsed time for failed request was " + (System.currentTimeMillis() - start) + " millis, url = "
                     + mutateUrl);
        throw e;
      }
    }
    logger.info("<!><!><!><!>test time elapsed after testing session #: " + session);
  }

  private String getSessionID(WebConversation wc) {
    return wc.getCookieValue("JSESSIONID");
  }

}
