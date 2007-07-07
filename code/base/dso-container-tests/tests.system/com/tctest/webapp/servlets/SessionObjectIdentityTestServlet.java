/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

    if (request.getRequestedSessionId() == null && session.isNew()) {
      if (s != null) {
        out.println("static field is non-null");
      }

      s = session;
      out.println("OK");
      return;
    }

    if (s != s) {
      out.println("session object is different reference on second request");
      return;
    }

    // Make sure that session object is portable and shared
    s.setAttribute("the session", s); // tests portability of session object
    if (!((Manageable) session).__tc_isManaged()) {
      out.println("session object itself is not a shared object");
    }

    out.println("OK");
  }

}