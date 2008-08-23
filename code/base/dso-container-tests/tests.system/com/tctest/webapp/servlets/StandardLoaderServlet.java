/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tc.object.loaders.NamedClassLoader;
import com.tc.util.Assert;
import com.tc.util.StringUtil;
import com.tctest.externall1.StandardClasspathDummyClass;
import com.tctest.externall1.StandardLoaderApp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StandardLoaderServlet extends HttpServlet {

  private final Map          sharedMap                           = new HashMap();
  public static final String GET_CLASS_LOADER_NAME               = "getClassLoaderName";
  public static final String PUT_INNER_INSTANCE                  = "putInnerInstance";
  public static final String CHECK_APP_INNER_INSTANCE            = "checkAppInnerInstance";
  public static final String PUT_STANDARD_LOADER_OBJECT_INSTANCE = "putStandardLoaderObjectInstance";

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String cmd = req.getParameter("cmd");
    if (GET_CLASS_LOADER_NAME.equals(cmd)) {
      NamedClassLoader loader = (NamedClassLoader) this.getClass().getClassLoader();
      resp.getWriter().print(loader.__tc_getClassLoaderName());
    } else if (PUT_INNER_INSTANCE.equals(cmd)) {
      synchronized (sharedMap) {
        sharedMap.put("1", new Inner());
        resp.getWriter().print("OK");
      }
    } else if (CHECK_APP_INNER_INSTANCE.equals(cmd)) {
      Object obj = sharedMap.get("2");
      if (obj instanceof StandardLoaderApp.AppInnerClass) {
        resp.getWriter().print("OK");
      }
      NamedClassLoader loader = (NamedClassLoader) this.getClass().getClassLoader();
      NamedClassLoader objClassLoader = (NamedClassLoader) obj.getClass().getClassLoader();

      Assert.assertEquals(loader, objClassLoader);

    } else if (PUT_STANDARD_LOADER_OBJECT_INSTANCE.equals(cmd)) {
      synchronized (sharedMap) {
        Object dummyObj = crateDummyObjectUsingSystemLoader();
        Assert.assertEquals("Object must be in standard class path", ClassLoader.getSystemClassLoader(), dummyObj
            .getClass().getClassLoader());
        sharedMap.put("3", dummyObj);
        resp.getWriter().print("OK");
      }
    } else if ("check".equals(cmd)) {
      printClassLoaderHierarchy(resp.getWriter());
    } else {
      resp.getWriter().print("Unknown cmd=" + StringUtil.safeToString(cmd));
    }
  }

  private Object crateDummyObjectUsingSystemLoader() {
    try {
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
      Class dummyClass = systemClassLoader.loadClass(StandardClasspathDummyClass.class.getName());
      return dummyClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void printClassLoaderHierarchy(PrintWriter pw) {
    ClassLoader classLoader = this.getClass().getClassLoader();
    while (classLoader != null) {
      pw.print(classLoader.getClass().getName());
      if (classLoader instanceof NamedClassLoader) {
        pw.println(" / " + ((NamedClassLoader) classLoader).__tc_getClassLoaderName());
      } else {
        pw.println(" / Not NamedClassLoader");
      }
      classLoader = classLoader.getParent();
    }
    String classpath = System.getProperty("java.class.path");
    pw.println(classpath);
  }

  public static class Inner {
    //
  }
}
