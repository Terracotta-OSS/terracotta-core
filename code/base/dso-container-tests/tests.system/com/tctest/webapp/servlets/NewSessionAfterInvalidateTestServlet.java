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

public final class NewSessionAfterInvalidateTestServlet extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    PrintWriter out = response.getWriter();

    int step = Integer.parseInt(request.getParameter("step"));

    if (step == 2) {
      String val = (String) session.getAttribute("key");
      // check that session was re-joined
      if (!"value2".equals(val)) {
        out.println("existing session has bad data: " + val);
        return;
      }
    }

    String firstId = session.getId();
    response.setContentType("text/html");

    session.setAttribute("key", "value");
    session.invalidate();

    HttpSession newSession = request.getSession(false);

    if (newSession != null) {
      out.println("getSession(false) returned a session after invalidate()");
      return;
    }

    newSession = request.getSession(true);

    if (newSession == session) {
      out.println("same session object returned");
      return;
    }

    if (newSession.getId().equals(firstId)) {
      out.println("session id resused for new session");
      return;
    }

    // test writing to the new session (would fail if lock not acquired)
    newSession.setAttribute("key", "value2");

    String value = (String) newSession.getAttribute("key");
    if ("value2".equals(value)) {
      out.println("OK");
    } else {
      out.println("Session has wrong value: " + value);
    }
  }

}