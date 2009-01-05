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

public class LongRunningRequestsServlet extends HttpServlet {

  public static final int    LONG_RUNNING_REQUEST_DURATION_SECS = 90;
  public static final String CREATE_SESSION                     = "createSession";
  public static final String LONG_RUNNING                       = "longRunning";
  public static final String NORMAL_SHORT_REQUEST               = "shortRequest";

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String cmd = req.getParameter("cmd");

    try {
      if (CREATE_SESSION.equals(cmd)) {
        HttpSession session = req.getSession(true);
        session.setAttribute("dumbValue", "dumbValue");
        resp.getWriter().print("OK");
        return;
      } else if (LONG_RUNNING.equals(cmd)) {
        debug("Serving " + LONG_RUNNING + " request: ");
        final long duration = LONG_RUNNING_REQUEST_DURATION_SECS * 1000;
        final long start = System.currentTimeMillis();
        while (true) {
          debug("Inside long running request...");
          HttpSession session = req.getSession(true);
          session.getAttribute("dumbValue");
          long remaining = System.currentTimeMillis() - (start + duration);
          if (remaining > 0) {
            break;
          }
          debug("Sleeping for 2 secs before continuing. Time left: " + remaining + " msecs");
          Thread.sleep(2000);
        }
        resp.getWriter().print("OK");
        return;
      } else if (NORMAL_SHORT_REQUEST.equals(cmd)) {
        debug("Serving " + NORMAL_SHORT_REQUEST + " request: ");
        HttpSession session = req.getSession(true);
        session.getAttribute("dumbValue");
        debug("After getting attribute from session in short request.");
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
