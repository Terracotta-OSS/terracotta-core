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

public final class SynchronousWriteTestServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    String serverParam = request.getParameter("server");
    String dataParam = request.getParameter("data");

    switch (Integer.parseInt(serverParam)) {
      case 0:
        hit0(session, out, dataParam);
        break;
      case 1:
        hit1(session, out, "data" + dataParam);
        break;
      default:
        out.print("unknown value for server param: " + serverParam);
    }
  }

  private void hit1(HttpSession session, PrintWriter out, String attrName) {
    System.err.println("### hit1: sessionId = " + session.getId());
    String value = (String) session.getAttribute(attrName);
    System.err.println(attrName + "=" + value);
    if (value == null) {
      out.print(attrName + " is null");
    } else {
      out.print(value);
    }
  }

  private void hit0(HttpSession session, PrintWriter out, String dataParam) {
    System.err.println("### hit0: sessionId = " + session.getId());
    System.err.println("setAttribute: " + "data" + dataParam);
    session.setAttribute("data" + dataParam, dataParam);
    out.print("OK");
  }
}