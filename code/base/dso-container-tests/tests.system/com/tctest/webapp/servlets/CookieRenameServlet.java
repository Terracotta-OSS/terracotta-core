/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tc.util.Assert;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CookieRenameServlet extends HttpServlet {
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("text/html");
    PrintWriter out = res.getWriter();

    String cmd = req.getParameter("cmd");

    if ("insert".equals(cmd) || "encode-url".equals(cmd)) {
      HttpSession session = req.getSession(true);
      Assert.assertEquals(null, req.getRequestedSessionId());
      Assert.assertEquals(false, req.isRequestedSessionIdValid());
      Assert.assertEquals(false, req.isRequestedSessionIdFromCookie());
      Assert.assertEquals(false, req.isRequestedSessionIdFromUrl());
      Assert.assertEquals(false, req.isRequestedSessionIdFromURL());
      session.setAttribute("hung", "daman");
      if ("insert".equals(cmd)) {
        out.println("OK");
      } else {
        out.println(res.encodeURL(req.getRequestURI()));
      }
    } else if ("query".equals(cmd)) {
      String source = req.getParameter("source");
      boolean cookie = "cookie".equals(source);
      boolean url = "url".equals(source);
      Assert.eval(cookie ^ url);
      HttpSession session = req.getSession(true);
      Assert.assertEquals(session.getId(), req.getRequestedSessionId());
      Assert.assertEquals(true, req.isRequestedSessionIdValid());
      Assert.assertEquals(cookie, req.isRequestedSessionIdFromCookie());
      Assert.assertEquals(url, req.isRequestedSessionIdFromUrl());
      Assert.assertEquals(url, req.isRequestedSessionIdFromURL());
      String data = (String) session.getAttribute("hung");
      if (data != null && data.equals("daman")) {
        out.println("OK");
      } else {
        out.println("ERROR: " + data);
      }
    } else if ("add-cookie".equals(cmd)) {
      String path = req.getParameter("add-cookie-path");
      String name = req.getParameter("add-cookie-name");
      String val = req.getParameter("add-cookie-value");
      Cookie cookie = new Cookie(name, val);
      cookie.setPath(path);
      res.addCookie(cookie);
      out.println("OK");
    } else if ("no-session".equals(cmd)) {
      String source = req.getParameter("source");
      boolean cookie = "cookie".equals(source);
      boolean url = "url".equals(source);
      HttpSession session = req.getSession(false);
      Assert.assertNull(session);
      Assert.assertEquals(req.getParameter("requested-id"), String.valueOf(req.getRequestedSessionId()));
      Assert.assertEquals(false, req.isRequestedSessionIdValid());
      Assert.assertEquals(cookie, req.isRequestedSessionIdFromCookie());
      Assert.assertEquals(url, req.isRequestedSessionIdFromUrl());
      Assert.assertEquals(url, req.isRequestedSessionIdFromURL());
      out.println("OK");
    } else {
      out.println("unknown cmd: " + cmd);
    }

  }
}
