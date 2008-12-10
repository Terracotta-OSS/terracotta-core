/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.util.concurrent.CyclicBarrier;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionLockingDeadlockServlet extends HttpServlet {

  public static final String  CREATE_SESSION           = "createSession";
  public static final String  LOCK_SESSION_THEN_GLOBAL = "lockSessionThenGlobal";
  public static final String  LOCK_GLOBAL_THEN_SESSION = "lockGlobalThenSession";

  private final Object        globalObject             = new Object();
  private final CyclicBarrier barrier                  = new CyclicBarrier(2);

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String cmd = req.getParameter("cmd");

    try {
      if (CREATE_SESSION.equals(cmd)) {
        HttpSession session = req.getSession(true);
        session.setAttribute("dumbValue", "dumbValue");
        resp.getWriter().print("OK");
        return;
      } else if (LOCK_SESSION_THEN_GLOBAL.equals(cmd)) {
        debug("Serving LOCK_SESSION_THEN_GLOBAL request: ");
        HttpSession session = req.getSession(true);
        session.getAttribute("dumbValue");
        debug("Got attribute. Calling await...");
        barrier.await();
        debug("... back from await");
        // wait for the other request to acquire the globalObject lock
        synchronized (globalObject) {
          debug("Did not deadlock!! Session-locking is working");
        }
        resp.getWriter().print("OK");
        return;
      } else if (LOCK_GLOBAL_THEN_SESSION.equals(cmd)) {
        debug("Serving LOCK_GLOBAL_THEN_SESSION request: ");
        debug("Synchronizing on globalObject...");
        synchronized (globalObject) {
          debug("Acquired lock on globalObject");
          debug("Calling await...");
          barrier.await();
          debug("... after await");
          HttpSession session = req.getSession(true);
          session.getAttribute("dumbValue");
          debug("Did not deadlock!! Session-locking is working");
        }
        resp.getWriter().print("OK");
        return;
      } else resp.getWriter().print("Unknown request: cmd=" + cmd);
    } catch (Exception e) {
      resp.getWriter().print("Exception from Server: " + e);
      e.printStackTrace();
    }
  }

  private static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

}
