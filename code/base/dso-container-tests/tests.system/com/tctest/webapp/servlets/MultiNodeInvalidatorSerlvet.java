/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tctest.webapp.listeners.MultiNodeInvalidatorListener;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class MultiNodeInvalidatorSerlvet extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    if (request.getParameter("query") != null) {
      out.println(MultiNodeInvalidatorListener.getNumberOfSessios());
      return;
    }

    HttpSession session = request.getSession(true);

    int idle = Integer.parseInt(request.getParameter("idle"));
    session.setMaxInactiveInterval(idle);

    Integer count = (Integer) session.getAttribute("count");
    if (count == null) {
      count = new Integer(0);
    }

    int newValue = count.intValue() + 1;
    session.setAttribute("count", new Integer(newValue));
    out.println(newValue);
  }
}