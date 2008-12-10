/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.test.server.util.TcConfigBuilder;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.servlets.LongRunningRequestsServlet;

import junit.framework.Test;

public class LongRunningRequestsTestWithSessionLocking extends LongRunningRequestsTestBase {

  public LongRunningRequestsTestWithSessionLocking() {
    disableAllUntil("2009-01-01");
  }

  public static Test suite() {
    return new LongRunningRequestsTestWithSessionLockingSetup();
  }

  public void testSessionLocking() throws Exception {
    WebConversation conversation = new WebConversation();
    Thread longRunningRequestThread = new Thread(new ParamBasedRequestRunner("cmd="
                                                                             + LongRunningRequestsServlet.LONG_RUNNING,
                                                                             server0, conversation));
    Thread[] shortRequestThreads = new Thread[5];
    for (int i = 0; i < shortRequestThreads.length; i++) {
      shortRequestThreads[i] = new Thread(
                                          new ParamBasedRequestRunner(
                                                                      "cmd="
                                                                          + LongRunningRequestsServlet.NORMAL_SHORT_REQUEST,
                                                                      server0, conversation));
    }
    super.testSessionLocking(conversation, longRunningRequestThread, shortRequestThreads);

    int waitTimeMillis = (LongRunningRequestsServlet.LONG_RUNNING_REQUEST_DURATION_SECS - 10) * 1000;
    ThreadUtil.reallySleep(waitTimeMillis);

    for (int i = 0; i < shortRequestThreads.length; i++) {
      if (shortRequestThreads[i].isAlive()) {
        Assert.fail("Short Requests are BLOCKED. Short Requests are NOT supposed to be blocked with session-locking");
      }
    }
    debug("Test passed");
  }

  private static class LongRunningRequestsTestWithSessionLockingSetup extends LongRunningRequestsTestSetupBase {

    public LongRunningRequestsTestWithSessionLockingSetup() {
      super(LongRunningRequestsTestWithSessionLocking.class, CONTEXT);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      tcConfigBuilder.addWebApplicationWithSessionLocking(CONTEXT);
    }
  }
}
