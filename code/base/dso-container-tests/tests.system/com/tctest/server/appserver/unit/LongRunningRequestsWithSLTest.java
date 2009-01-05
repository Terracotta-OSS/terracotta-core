/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.servlets.LongRunningRequestsServlet;

import junit.framework.Test;

public class LongRunningRequestsWithSLTest extends LongRunningRequestsTestBase {

  public LongRunningRequestsWithSLTest() {
    //
  }

  public static Test suite() {
    return new LongRunningRequestsTestWithSessionLockingSetup();
  }

  public void testSessionLocking() throws Exception {
    WebConversation conversation = new WebConversation();
    Thread longRunningRequestThread = new Thread(
                                                 new ParamBasedRequestRunner(
                                                                             server0,
                                                                             conversation,
                                                                             CONTEXT,
                                                                             "cmd="
                                                                                 + LongRunningRequestsServlet.LONG_RUNNING));
    Thread[] shortRequestThreads = new Thread[5];
    for (int i = 0; i < shortRequestThreads.length; i++) {
      shortRequestThreads[i] = new Thread(
                                          new ParamBasedRequestRunner(
                                                                      server0,
                                                                      conversation,
                                                                      CONTEXT,
                                                                      "cmd="
                                                                          + LongRunningRequestsServlet.NORMAL_SHORT_REQUEST));
    }
    super.testSessionLocking(conversation, longRunningRequestThread, shortRequestThreads);

    int waitTimeMillis = (LongRunningRequestsServlet.LONG_RUNNING_REQUEST_DURATION_SECS - 10) * 1000;
    ThreadUtil.reallySleep(waitTimeMillis);

    for (int i = 0; i < shortRequestThreads.length; i++) {
      if (!shortRequestThreads[i].isAlive()) {
        Assert
            .fail("Short Requests are NOT blocked. Short Requests are supposed to be blocked with session-locking=true");
      }
    }
    debug("Test passed");
  }

  private static class LongRunningRequestsTestWithSessionLockingSetup extends LongRunningRequestsTestSetupBase {

    public LongRunningRequestsTestWithSessionLockingSetup() {
      super(LongRunningRequestsWithSLTest.class, CONTEXT);
    }
  }
}
