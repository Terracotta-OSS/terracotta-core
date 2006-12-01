/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.servlets;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SanityCheckServlet extends HttpServlet {
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    HttpSession session = req.getSession();
    res.getWriter().print(session.getId());
    
    String val = (String) session.getAttribute("count");
    if (val == null) val = "0";
    
    res.getWriter().print("count=" + val);
    
    int count = Integer.parseInt(val);
    session.setAttribute("count", "" + ++count);
  }
}
