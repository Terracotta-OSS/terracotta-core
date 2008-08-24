/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servletfilters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SimpleFilter implements Filter {
  public static final String ATTR_NAME  = SimpleFilter.class.getName();
  public static final String ATTR_VALUE = "hi there";

  public void doFilter(final ServletRequest request, final ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    System.out.println("Entering Filter");

    request.setAttribute(ATTR_NAME, ATTR_VALUE);

    HttpSession session = ((HttpServletRequest) request).getSession(true);
    System.out.println("SESSION ID: " + session.getId());

    String cmd = request.getParameter("cmd");
    if ("insert".equals(cmd)) {
      session.setAttribute("SimpleFilter", "value");
    } else if ("query".equals(cmd)) {
      String value = (String) session.getAttribute("SimpleFilter");
      if (!"value".equals(value)) {
        // If you're getting null here, the session probably isn't clustered. Is this filter before the TC session
        // filter for some reason? (it shouldn't be)
        throw new AssertionError("unexpected value in session: " + value);
      }
    } else {
      throw new AssertionError("unknown cmd: " + cmd);
    }

    chain.doFilter(request, response);
    System.out.println("Exiting Filter");
  }

  public void init(FilterConfig config) {
    System.out.println("Filter Initialized");
  }

  public void destroy() {
    System.out.println("Filter Destroyed");
  }
}
