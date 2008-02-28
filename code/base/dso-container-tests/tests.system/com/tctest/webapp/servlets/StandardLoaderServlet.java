/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tc.object.loaders.NamedClassLoader;
import com.tc.util.StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StandardLoaderServlet extends HttpServlet {

  private final Map sharedMap = new HashMap();

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String cmd = req.getParameter("cmd");
    if ("getClassLoaderName".equals(cmd)) {
      NamedClassLoader loader = (NamedClassLoader) this.getClass().getClassLoader();
      resp.getWriter().print(loader.__tc_getClassLoaderName());
    } else if ("putInstance".equals(cmd)) {
      synchronized (sharedMap) {
        sharedMap.put("1", new Inner());
        resp.getWriter().print("OK");
      }
    } else {
      resp.getWriter().print("Unknown cmd=" + StringUtil.safeToString(cmd));
    }
  }

  public static class Inner {
    //
  }
}
