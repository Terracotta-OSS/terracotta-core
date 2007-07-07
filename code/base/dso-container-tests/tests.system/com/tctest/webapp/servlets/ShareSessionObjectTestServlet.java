/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class ShareSessionObjectTestServlet extends HttpServlet {

  private static final String ATTR_NAME = "session reference";

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    PrintWriter out = response.getWriter();

    try {
      doGet0(request, response);
      out.write("OK");
    } catch (Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      out.write(sw.toString());
    }
  }

  private void doGet0(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");

    String cmd = request.getParameter("cmd");

    Object o = session.getAttribute(ATTR_NAME);
    if ("set".equals(cmd)) {
      if (o != null) { throw new AssertionError("attribute is not null: " + o); }
      session.setAttribute(ATTR_NAME, session);
    } else if ("read".equals(cmd)) {
      if (o != session) { throw new AssertionError("different session objects, " + session + " != " + o); }
    } else {
      throw new AssertionError("unknown cmd: " + cmd);
    }

  }
}