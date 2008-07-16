/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResponseIsCommittedServlet extends HttpServlet {

  // not using anything 400 or above since it trips up HttpURLConnection
  public static final int           SEND_ERROR_CODE = 399;

  private static final WasCommitted wasCommitted    = new WasCommitted();

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // This sync block is to keep this entire method atomic. After calls to sendRedirect()/sendError() the test program
    // will continue and will race with setting the committed flags
    synchronized (wasCommitted) {
      resp.setContentType("text/html");
      PrintWriter out = resp.getWriter();

      String cmd = req.getParameter("cmd");
      if (!cmd.startsWith("check-")) {
        wasCommitted.checkEmpty(cmd);
      } else {
        out.print(wasCommitted.get(cmd.substring("check-".length())));
        return;
      }

      if ("sendRedirect".equals(cmd)) {
        resp.sendRedirect("http://www.google.com/DOESNT_MATTER");
        wasCommitted.set(cmd, resp.isCommitted());
      } else if ("sendError1".equals(cmd)) {
        resp.sendError(SEND_ERROR_CODE);
        wasCommitted.set(cmd, resp.isCommitted());
      } else if ("sendError2".equals(cmd)) {
        resp.sendError(SEND_ERROR_CODE, "error message");
        wasCommitted.set(cmd, resp.isCommitted());
      } else {
        throw new AssertionError("unknown command: " + cmd);
      }
    }
  }

  private static class WasCommitted {

    private final Map values = new HashMap();

    synchronized void set(String cmd, boolean isCommitted) {
      checkEmpty(cmd);
      Boolean value = Boolean.valueOf(isCommitted);
      values.put(cmd, value);
      System.err.println("set value [" + value + "] for command=[" + cmd + "]. Map is " + values + " @ "
                         + System.identityHashCode(values));
    }

    synchronized void checkEmpty(String cmd) {
      Boolean b = (Boolean) values.get(cmd);
      if (b != null) { throw new AssertionError("existing value for command " + cmd + ": " + b); }
    }

    synchronized boolean get(String cmd) {
      Boolean b = (Boolean) values.get(cmd);
      if (b == null) { throw new AssertionError("missing value for command=[" + cmd + "]. Map has " + values + " @ "
                                                + System.identityHashCode(values)); }
      return b.booleanValue();
    }
  }

}
