/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ErrorServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PrintWriter writer = resp.getWriter();

    HttpSession session = req.getSession(true);

    // This was failing under weblogic 9+ since we didn't set <dispatcher> on the session filter (thus the native
    // request object was seen here)
    if (!session.getClass().getName().startsWith("com.terracotta.session")) {
      writer.println("unexpected session type: " + session.getClass().getName());
      return;
    }

    writer.println("OK");
  }
}
