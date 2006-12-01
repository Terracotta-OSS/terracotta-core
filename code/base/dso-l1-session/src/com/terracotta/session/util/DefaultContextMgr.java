/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionContext;

public class DefaultContextMgr implements ContextMgr {

  private final ServletContext servletContext;
  private final String         appName;

  public static DefaultContextMgr makeInstance(HttpServletRequest req, ServletContext servletContext) {
    return new DefaultContextMgr(computeAppName(req), servletContext);
  }
  
  public static DefaultContextMgr makeInstance(String contextPath, ServletContext servletContext) {
    return new DefaultContextMgr(computeAppName(contextPath), servletContext);
  }
  
  protected DefaultContextMgr(String appName, ServletContext servletContext) {
    Assert.pre(appName != null);
    this.servletContext = servletContext;
    this.appName = appName;
  }

  public ServletContext getServletContext() {
    return servletContext;
  }

  public HttpSessionContext getSessionContext() {
    return DefaultSessionContext.theInstance;
  }

  public String getAppName() {
    Assert.post(appName != null);
    return appName;
  }

  public static String computeAppName(HttpServletRequest request) {
    Assert.pre(request != null);
    String app = request.getContextPath();
    return computeAppName(app);
  }

  private static String computeAppName(String app) {
    // compute app name
    // deal with possible app strings: null, "", "/", "/xyz", "xyz/", "/xyz/"
    if (app == null) app = "";
    else app = app.trim();
    if (app.length() == 0 || "/".equals(app)) return "ROOT";
    if (app.startsWith("/")) app = app.substring(1);
    if (app.endsWith("/")) app = app.substring(0, app.length() - 2);
    Assert.post(app != null);
    return app;
  }
}
