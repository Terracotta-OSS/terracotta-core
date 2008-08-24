/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tctest.webapp.servletfilters.SimpleFilter;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SimpleSessionFilterServlet extends ShutdownNormallyServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    super.doGet(request, response);

    String attr = (String) request.getAttribute(SimpleFilter.ATTR_NAME);

    if (!SimpleFilter.ATTR_VALUE.equals(attr)) {
      // a little sanity checking for this test. The filter should add this attribute in the request
      throw new AssertionError("unexpected request attribute [" + attr + "] -- Is the filter present?");
    }

  }

}
