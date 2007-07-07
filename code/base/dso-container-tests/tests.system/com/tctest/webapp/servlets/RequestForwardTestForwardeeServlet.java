/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class RequestForwardTestForwardeeServlet extends HttpServlet {
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    final String action = req.getParameter("action");
    System.err.println("### ForwardEEServlet.doGet is here...");
    String reply = "FORWARD OK";
    if (action.endsWith("s")) {
      req.getSession();
      reply= "FORWARD GOT SESSION";
    } else if (action.endsWith("n")) {
      reply= "FORWARD DID NOT GET SESSION";
    } else {
      reply = "INVALID REQUEST";
    }
    resp.getWriter().print(reply);
    resp.flushBuffer();
  }
}