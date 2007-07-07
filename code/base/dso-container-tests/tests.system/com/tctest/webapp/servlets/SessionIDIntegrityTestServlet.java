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

public final class SessionIDIntegrityTestServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    String cmdParam = request.getParameter("cmd");
    if ("insert".equals(cmdParam)) {
      session.setAttribute("hung", "daman");
      out.println("OK");
    } else if ("query".equals(cmdParam)) {
      String data = (String) session.getAttribute("hung");
      if (data != null && data.equals("daman")) {
        out.println("OK");
      } else {
        out.println("ERROR: " + data);
      }
    } else {
      out.println("unknown cmd");
    }

  }
}