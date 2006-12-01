/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class WebAppServlet extends HttpServlet {
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    final String op = req.getParameter("op");
    if ("create".equals(op)) {
      doCreate(req, res);
    } else {
      res.getWriter().print("INVALID operation");
      res.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
  }

  private static void doCreate(HttpServletRequest req, HttpServletResponse res) throws IOException {
    HttpSession sess = req.getSession();
    res.getWriter().print("SessionId=" + sess.getId());
    res.setStatus(HttpServletResponse.SC_OK);
  }
}
