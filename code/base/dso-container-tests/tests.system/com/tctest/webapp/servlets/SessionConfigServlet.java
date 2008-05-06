/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class SessionConfigServlet extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    
    String testCase = request.getParameter("testcase");
    if ("testCookieDisabled".equals(testCase)) {
      // cookie enable set to false so we should get no cookies
      // on second hit (hit=1)
      String hit = request.getParameter("hit");
      if ("1".equals(hit) && request.getCookies() != null) {
        out.println("Still receive cookie when CookiesEnabled=false");
      }
    } else if ("testUrlRewritingDisabled".equals(testCase)) {
      String originalUrl = request.getRequestURI() + "/index.html";
      String encodedUrl = response.encodeURL(originalUrl);
      if (!originalUrl.equals(encodedUrl)) {
        out.println("encodeUrl succeeded: " + encodedUrl);
      }
    } else if ("testTrackingDisabled".equals(testCase)) {
      String hit = request.getParameter("hit");
      if ("1".equals(hit) && request.getCookies() != null) {
        out.println("Still receive cookie when TrackingDisabled=false");
      }
      String originalUrl = request.getRequestURI() + "/index.html";
      String encodedUrl = response.encodeURL(originalUrl);
      if (!originalUrl.equals(encodedUrl)) {
        out.println("encodeUrl succeeded: " + encodedUrl);
      }
    } else if ("testSessionTimeOutArbitrary".equals(testCase)) {
      out.println(session.getMaxInactiveInterval());
    } else if ("testSessionTimeOutNegative".equals(testCase)) {
      int hit = Integer.parseInt(request.getParameter("hit"));
      if (hit == 0) {
        out.println(session.getMaxInactiveInterval());
        session.setAttribute("testSessionTimeOutNegative", "0");
      } else if (hit == 1) {
        if (session.getAttribute("testSessionTimeOutNegative") == null) {
          out.println("session has expired. isNew() returns " + session.isNew());
        }
      }
    } else if ("testResetTimeoutToLowerValue".equals(testCase)) {
      int hit = Integer.parseInt(request.getParameter("hit"));
      int timeout;
      switch (hit) {
        case 0:
          timeout = Integer.parseInt(request.getParameter("timeout"));
          session.setMaxInactiveInterval(timeout);
          break;
        case 1:
          timeout = Integer.parseInt(request.getParameter("timeout"));
          session.setMaxInactiveInterval(timeout);
          session.setAttribute("value", request.getParameter("value"));
          break;
        case 2:
          String value = request.getParameter("value");
          String storedValue = (String)session.getAttribute("value");
          if (!value.equals(storedValue)) {
            out.println("expected <" + value + "> but got <" + storedValue + ">");
          }
          break;
        default:
          out.println("Wrong hit");
      }
    }
    out.println("OK");
  }
}
