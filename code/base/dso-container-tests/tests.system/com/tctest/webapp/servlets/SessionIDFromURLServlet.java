/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tc.util.Assert;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class SessionIDFromURLServlet extends HttpServlet {

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();

    String cmd = req.getParameter("cmd");
    if (cmd == null) { throw new AssertionError("missing cmd"); }

    if ("new".equals(cmd)) {
      Assert.assertFalse(req.isRequestedSessionIdValid());
      Assert.assertFalse(req.isRequestedSessionIdFromCookie());
      Assert.assertFalse(req.isRequestedSessionIdFromUrl());
      Assert.assertFalse(req.isRequestedSessionIdFromURL());
      Assert.assertNull(req.getRequestedSessionId(), req.getRequestedSessionId());

      HttpSession session = req.getSession(true);
      Assert.assertTrue(session.isNew());

      out.println(resp.encodeURL(req.getRequestURL().toString()));
    } else if ("query".equals(cmd)) {
      Assert.assertTrue(req.isRequestedSessionIdValid());
      Assert.assertFalse(req.isRequestedSessionIdFromCookie());
      Assert.assertTrue(req.isRequestedSessionIdFromUrl());
      Assert.assertTrue(req.isRequestedSessionIdFromURL());

      HttpSession session = req.getSession(false);
      Assert.assertFalse(session.isNew());

      Assert.assertEquals(req.getRequestedSessionId(), session.getId());

      out.println("OK");
    } else {
      throw new AssertionError("unknown cmd: " + cmd);
    }

  }

}
