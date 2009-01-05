/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.servlets.ManualSessionLockingServlet;

import junit.framework.Test;

public class SessionLockingTrueTest extends ManualSessionLockingTestBase {

  public static Test suite() {
    return new SessionLockingTrueTestSetup();
  }

  public void testSesionLocking() throws Exception {
    WebConversation conversation = new WebConversation();

    Thread waitingRequestThread = new Thread(new ParamBasedRequestRunner(server0, conversation, CONTEXT,
                                                                         "cmd=" + ManualSessionLockingServlet.WAIT));
    Thread notifyingRequestThread = new Thread(new ParamBasedRequestRunner(server0, conversation, CONTEXT,
                                                                           "cmd=" + ManualSessionLockingServlet.NOTIFY));
    super.testSessionLocking(conversation, waitingRequestThread, notifyingRequestThread);

    int waitTimeMillis = 30 * 1000;
    ThreadUtil.reallySleep(waitTimeMillis);

    if (!waitingRequestThread.isAlive() || !notifyingRequestThread.isAlive()) {
      Assert.fail("Requests are NOT blocked. Request are supposed to be blocked with session-locking=true");
    }
    debug("Requests are BLOCKED with session-locking=true.");
    debug("Test passed");
  }

  private static class SessionLockingTrueTestSetup extends ManualSessionLockingSetup {

    public SessionLockingTrueTestSetup() {
      super(SessionLockingTrueTest.class, CONTEXT);
    }
  }
}
