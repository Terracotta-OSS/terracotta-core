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

public final class StickySessionServlet extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    String instance = System.getProperty("app_instance");
    String session_instance = (String) session.getAttribute("instance");

    boolean isSticky = true;
    if (session_instance != null && !session_instance.equals(instance)) isSticky = false;
    session.setAttribute("instance", instance);

    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println(isSticky);
    System.out.println(session.getId() + ": " + isSticky);
  }
}