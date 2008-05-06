/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tc.object.bytecode.Manageable;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class SessionObjectIdentityTestServlet extends HttpServlet {

  private static volatile HttpSession s = null;

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    PrintWriter out = response.getWriter();

    String cmd = request.getParameter("cmd");

    if ("create".equals(cmd)) {
      if (request.getRequestedSessionId() == null && session.isNew()) {
        if (s != null) {
          out.println("static field is non-null");
          return;
        }
        s = session;
      } else {
        out.println("session already exists");
        return;
      }
    } else if ("checkIdentity".equals(cmd)) {
      if (s == null) {
        out.println("static field is null");
        return;
      }
      if (s != session) {
        out.println("session object is different reference");
        return;
      }
    } else if ("shareSession".equals(cmd)) {
      // Make sure that session object is portable and shared
      session.setAttribute("the session", session); // tests portability of session object
      if (!((Manageable) session).__tc_isManaged()) {
        out.println("session object itself is not a shared object");
        return;
      }
    } else if ("checkShared".equals(cmd)) {
      HttpSession fromSession = (HttpSession) session.getAttribute("the session");
      if (fromSession != session) {
        out.println("session reference is different");
        return;
      }

      // interact with the shared session for good measure
      fromSession.setAttribute("blah", "blah");
    } else {
      out.println("unknown command: " + cmd);
    }

    out.println("OK");
  }

}