/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ManualSessionLockingServlet extends HttpServlet {

  public static final String CREATE_SESSION = "createSession";
  public static final String WAIT           = "wait";
  public static final String NOTIFY         = "notify";
  public static final String KEY            = "key";

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String cmd = req.getParameter("cmd");

    HttpSession session = req.getSession(true);
    String sessionId = session.getId();
    debug("Current sessionId: " + sessionId + " cmd=" + cmd);

    if (WAIT.equals(cmd)) {
      debug("Serving waiting request: ");
      while (true) {
        String value = (String) session.getAttribute(KEY);
        if (value == null) {
          debug("Using session: " + session + ". Found null for key=" + KEY
                + ". Waiting for other request to put in value...");
        } else {
          debug("Using session: " + session + " found " + value + " for key=" + KEY);
          break;
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          debug("Caught: " + e);
        }
      }
      debug("Got notified");
      resp.getWriter().print("OK");
      return;
    } else if (NOTIFY.equals(cmd)) {
      debug("Adding value for key=" + KEY + " in session:" + session);
      session.setAttribute(KEY, "Some Object");
      resp.getWriter().print("OK");
      return;
    } else if (CREATE_SESSION.equals(cmd)) {
      session.setAttribute("dumbValue", "dumbValue");
      resp.getWriter().print("OK");
      return;
    }
    resp.getWriter().print("Unknown request: cmd=" + cmd);
  }

  private static void debug(String string) {
    System.out.println(Thread.currentThread().getName() + ": " + string);
  }

}
