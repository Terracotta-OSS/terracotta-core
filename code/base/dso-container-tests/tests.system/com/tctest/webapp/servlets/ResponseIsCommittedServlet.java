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

  private static final WasCommitted wasCommitted = new WasCommitted();

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      wasCommitted.set(cmd, resp.isCommitted());
    } else if ("sendError2".equals(cmd)) {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "error message");
      wasCommitted.set(cmd, resp.isCommitted());
    } else {
      throw new AssertionError("unknown command: " + cmd);
    }
  }

  private static class WasCommitted {

    private final Map values = new HashMap();

    synchronized void set(String cmd, boolean isCommitted) {
      checkEmpty(cmd);
      values.put(cmd, Boolean.valueOf(isCommitted));
    }

    synchronized void checkEmpty(String cmd) {
      Boolean b = (Boolean) values.get(cmd);
      if (b != null) { throw new AssertionError("existing value for command " + cmd + ": " + b); }
    }

    synchronized boolean get(String cmd) {
      Boolean b = (Boolean) values.get(cmd);
      if (b == null) { throw new AssertionError("missing value for command " + cmd); }
      return b.booleanValue();
    }
  }

}
