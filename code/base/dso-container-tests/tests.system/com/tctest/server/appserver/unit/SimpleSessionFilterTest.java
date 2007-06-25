/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.tc.test.server.appserver.unit.TCServletFilter;
import com.tc.util.runtime.Vm;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Applies a simple filter to the {@link SimpleDsoSessionsTest}.
 */
public final class SimpleSessionFilterTest extends SimpleDsoSessionsTest {

  public SimpleSessionFilterTest() {
    if (Vm.isIBM()) {
      disableAllUntil("2007-07-03");
    }
  }

  public static final class SimpleFilter implements TCServletFilter {

    public String getPattern() {
      return "/*";
    }

    public Map getInitParams() {
      return null;
    }

    public void doFilter(final ServletRequest request, final ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

      System.out.println("Entering Filter");
      HttpSession session = ((HttpServletRequest) request).getSession(true);
      System.out.println("SESSION ID: " + session.getId());
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
}
