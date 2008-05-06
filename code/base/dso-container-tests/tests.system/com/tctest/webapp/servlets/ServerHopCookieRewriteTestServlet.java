/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class ServerHopCookieRewriteTestServlet extends HttpServlet {

  private static final String ATTR_NAME   = "test-attribute";
  private static final String SESS_ID     = "session-id";

  public static final String  DEFAULT_DLM = "!";

  private String              dlm;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    if ((dlm = getInitParameter("session.delimiter")) == null) {
      dlm = DEFAULT_DLM;
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    String serverParam = request.getParameter("server");
    if ("0".equals(serverParam)) {
      hit0(request, session, out);
    } else if ("1".equals(serverParam)) {
      hit1(request, session, out);
    } else if ("2".equals(serverParam)) {
      hit2(request, session, out);
    } else if ("3".equals(serverParam)) {
      hit3(request, session, out);
    } else {
      out.print("unknown value for server param: " + serverParam);
    }
  }

  private void hit3(HttpServletRequest request, HttpSession session, PrintWriter out) {
    if (session.isNew()) {
      out.print("session is new for server 3; sessionId=" + session.getId());
    } else if (isServerHop(session)) {
      out.print("is a server hop");
    } else {
      out.println("OK");
    }
  }

  private void hit1(HttpServletRequest req, HttpSession session, PrintWriter out) {
    if (session.isNew()) {
      out.print("session is new for server 1; sessionId=" + session.getId());
    } else if (!isServerHop(session)) {
      out.print("not a server hop");
    } else {
      String value = (String) session.getAttribute(ATTR_NAME);
      if (value == null) {
        out.print("attribute is null");
      } else {
        if (!value.equals("0")) {
          out.print("unexpected value: " + value);
        }
        final String oldKey = getKey((String) session.getAttribute(SESS_ID));
        final String newKey = getKey(session.getId());
        final String oldServerId = getServerId((String) session.getAttribute(SESS_ID));
        final String newServerId = getServerId(session.getId());

        System.out.println("session.getId()='" + session.getId() + "'");
        System.out.println("oldKey='" + oldKey + "' newKey='" + newKey + "' oldServerId='" + oldServerId
                           + "' newServerId='" + newServerId + "'");

        if (!session.getId().equals(req.getRequestedSessionId()) && newKey.equals(oldKey)
            && !newServerId.equals(oldServerId)) {
          out.print("OK");
        } else {
          out.print("oldKey=" + oldKey + ",newKey=" + newKey + ",oldServerId=" + oldServerId + ",newServerId="
                    + newServerId);
        }
      }
    }
  }

  private String getServerId(String s) {
    if (s == null || s.trim().length() == 0 || s.indexOf(dlm) == -1) return null;
    return s.substring(s.indexOf(dlm) + 1);
  }

  private String getKey(String s) {
    if (s == null || s.trim().length() == 0 || s.indexOf(dlm) == -1) return null;
    return s.substring(0, s.indexOf(dlm));
  }

  private void hit2(HttpServletRequest req, HttpSession session, PrintWriter out) {
    if (session.isNew()) {
      out.print("session is  new for server 2; sessionId=" + session.getId());
    } else if (!isServerHop(session)) {
      out.print("not a server hop");
    } else {
      String value = (String) session.getAttribute(ATTR_NAME);
      if (value == null) {
        out.print("missing attribute");
      }
      value = (String) session.getAttribute(SESS_ID);
      if (!session.getId().equals(req.getRequestedSessionId()) && session.getId().equals(value)) {
        out.print("OK");
      } else {
        out.print("expected=" + session.getId() + ",got=" + value);
      }
    }
  }

  private void hit0(HttpServletRequest req, HttpSession session, PrintWriter out) {
    if (!session.isNew()) {
      out.print("session is not new for server 0; sessionId=" + session.getId());
      return;
    }

    String value = (String) session.getAttribute(ATTR_NAME);
    if (value != null) {
      out.print("attribute already exists: " + value);
      return;
    }

    if (!session.getId().equals(req.getRequestedSessionId())) {
      session.setAttribute(ATTR_NAME, "0");
      session.setAttribute(SESS_ID, session.getId());
      out.print("OK");
    } else {
      out.print("requested session ID equals actual session ID: " + session.getId());
    }
  }

  private static boolean isServerHop(HttpSession session) {
    try {
      // TC Sessions
      Object sessionId = session.getClass().getMethod("getSessionId", new Class[] {}).invoke(session, new Object[] {});
      Boolean b = (Boolean) sessionId.getClass().getMethod("isServerHop", new Class[] {}).invoke(sessionId,
                                                                                                 new Object[] {});
      return b.booleanValue();
    } catch (Exception e) {
      try {
        // Jetty Sessions
        Boolean b = (Boolean) session.getClass().getMethod("isServerHop", new Class[] {}).invoke(session,
                                                                                                 new Object[] {});
        return b.booleanValue();
      } catch (Exception e2) {
        /**/
      }
      throw new RuntimeException(e);
    }

  }

  // if (!((SessionData) session).getSessionId().isServerHop()) {

}