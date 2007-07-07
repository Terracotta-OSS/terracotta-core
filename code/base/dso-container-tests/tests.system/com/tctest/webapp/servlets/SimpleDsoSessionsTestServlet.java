/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class SimpleDsoSessionsTestServlet extends HttpServlet {

  private static final String ATTR_NAME = "test-attribute";

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    String serverParam = request.getParameter("server");
    if ("0".equals(serverParam)) {
      hit0(session, out);
    } else if ("1".equals(serverParam)) {
      hit1(session, out);
    } else {
      out.print("unknown value for server param: " + serverParam);
    }
  }

  private void hit1(HttpSession session, PrintWriter out) {
    System.err.println("### hit1: sessionId = " + session.getId());
    if (session.isNew()) {
      out.print("session is new for server 1; sessionId=" + session.getId());
    } else {
      String value = (String) session.getAttribute(ATTR_NAME);
      if (value == null) {
        out.print("attribute is null");
      } else {
        if (value.equals("0")) {
          out.print("OK");
        } else {
          out.print("unexpected value: " + value);
        }
      }
    }
  }

  private void hit0(HttpSession session, PrintWriter out) {
    if (!session.isNew()) {
      out.print("session is not new for server 0; sessionId=" + session.getId());
      return;
    }

    System.err.println("### hit0: sessionId = " + session.getId());

    String value = (String) session.getAttribute(ATTR_NAME);
    if (value == null) {
      out.print("OK");
      session.setAttribute(ATTR_NAME, "0");
    } else {
      out.print("attribute already exists: " + value);
    }
  }
}