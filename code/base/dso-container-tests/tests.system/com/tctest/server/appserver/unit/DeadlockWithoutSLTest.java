/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.webapp.servlets.SessionLockingDeadlockServlet;

import junit.framework.Test;

public class DeadlockWithoutSLTest extends DeadlockTestBase {

  public DeadlockWithoutSLTest() {
    // disableAllUntil("2009-01-01");
  }

  public static Test suite() {
    return new DeadlockTestWithoutSessionLockingSetup();
  }

  public void testSessionLocking() throws Exception {
    WebConversation conversation = new WebConversation();
    Thread requestSessionThenGlobalThread = new Thread(
                                                       new ParamBasedRequestRunner(
                                                                                   server0,
                                                                                   conversation,
                                                                                   CONTEXT,
                                                                                   "cmd="
                                                                                       + SessionLockingDeadlockServlet.LOCK_SESSION_THEN_GLOBAL));
    Thread requestGlobalThenSessionThread = new Thread(
                                                       new ParamBasedRequestRunner(
                                                                                   server0,
                                                                                   conversation,
                                                                                   CONTEXT,
                                                                                   "cmd="
                                                                                       + SessionLockingDeadlockServlet.LOCK_GLOBAL_THEN_SESSION));
    super.testSessionLocking(conversation, requestSessionThenGlobalThread, requestGlobalThenSessionThread);

    int waitTimeMillis = 30 * 1000;
    ThreadUtil.reallySleep(waitTimeMillis);

    if (requestSessionThenGlobalThread.isAlive()) {
      requestSessionThenGlobalThread.interrupt();
      if (requestGlobalThenSessionThread.isAlive()) {
        requestGlobalThenSessionThread.interrupt();
      }
      Assert
          .fail("Requests are deadlocked. Waiting Request did not complete, requests should not deadlock when session-locking=false");
    }
    debug("Test passed");
  }

  private static class DeadlockTestWithoutSessionLockingSetup extends DeadlockTestSetupBase {

    public DeadlockTestWithoutSessionLockingSetup() {
      super(DeadlockWithoutSLTest.class, CONTEXT);
    }

    @Override
    public boolean isSessionLockingTrue() {
      return false;
    }

  }
}
