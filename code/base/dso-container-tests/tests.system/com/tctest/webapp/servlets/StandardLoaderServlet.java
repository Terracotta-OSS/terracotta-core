/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StandardLoaderServlet extends HttpServlet {

  private final Map sharedMap = new HashMap();

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    synchronized (sharedMap) {
      sharedMap.put("1", new Inner());
    }
    resp.getWriter().print("OK");
  }

  static class Inner {
    //
  }
}
