/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class RequestForwardTestForwarderServlet extends HttpServlet {
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    final String action = req.getParameter("action");
    final String target = req.getParameter("target");
    String reply = "OK";
    RequestDispatcher requestDispatcher = req.getRequestDispatcher(target);
    System.err.println("\n%%% ForwarderServlet.doGet is here...action=" + action + ", target=" + target);
    
    if ("s-f-s".equals(action)) {
      req.getSession();
      requestDispatcher.forward(req, resp);
      req.getSession();
    } else if ("n-f-s".equals(action)) {
      requestDispatcher.forward(req, resp);
      req.getSession();
    } else if ("s-f-n".equals(action)) {
      req.getSession();
      System.err.println("%%% ForwarderServlet: calling forward ...");
      requestDispatcher.forward(req, resp);
      System.err.println("%%% ForwarderServlet: returned from forward forward ...");
    } else {
      reply = "INVALID REQUEST";
      resp.getWriter().print(reply);
      resp.flushBuffer();
    }
  }
}