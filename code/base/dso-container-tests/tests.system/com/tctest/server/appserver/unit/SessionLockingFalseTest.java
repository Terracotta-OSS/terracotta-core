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

public class SessionLockingFalseTest extends ManualSessionLockingTestBase {

  public static Test suite() {
    return new SessionLockingFalseTestSetup();
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

    if (waitingRequestThread.isAlive()) {
      waitingRequestThread.interrupt();
      if (notifyingRequestThread.isAlive()) {
        notifyingRequestThread.interrupt();
      }
      Assert.fail("Requests are BLOCKED. Requests are NOT supposed to block with session-locking=false");
    }
    debug("Requests are NOT BLOCKED with session-locking=false.");
    debug("Test passed");
  }

  private static class SessionLockingFalseTestSetup extends ManualSessionLockingSetup {

    public SessionLockingFalseTestSetup() {
      super(SessionLockingFalseTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
