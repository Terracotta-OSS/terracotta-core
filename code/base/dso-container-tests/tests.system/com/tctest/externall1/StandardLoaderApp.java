/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.externall1;

import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.tools.BootJarTool;
import com.tc.util.Assert;
import com.tctest.webapp.StandardClasspathDummyClass;
import com.tctest.webapp.servlets.StandardLoaderServlet;

import java.util.HashMap;
import java.util.Map;

public class StandardLoaderApp {

  private final Map sharedMap = new HashMap();

  public static void main(String[] args) {
    try {
      checkStandardLoaderName();

      checkSetLoaderName();

      final StandardLoaderApp app = new StandardLoaderApp();

      app.doTest();

      synchronized (app.sharedMap) {
        app.sharedMap.put("2", new AppInnerClass());
      }

      System.out.println("OK");

    } catch (Exception e) {
      System.exit(1);
    }
  }

  private static void checkStandardLoaderName() {
    NamedClassLoader loader = (NamedClassLoader)ClassLoader.getSystemClassLoader();
    String expectedLoaderName = System.getProperty(BootJarTool.SYSTEM_CLASSLOADER_NAME_PROPERTY);
    Assert.assertNotNull("Expected Sytem class loader name", expectedLoaderName);
    Assert.assertEquals(expectedLoaderName, loader.__tc_getClassLoaderName());
  }

  private static void checkSetLoaderName() {
    try {
      NamedClassLoader loader = (NamedClassLoader)ClassLoader.getSystemClassLoader();
      loader.__tc_setClassLoaderName("someName");
      Assert.fail("__tc_setClassLoaderName() should throw Assertion error.");
    } catch (AssertionError e) {
      //ok
    }
  }

  private void doTest() {
    synchronized (sharedMap) {
      Object obj = sharedMap.get("1");
      if (!(obj instanceof StandardLoaderServlet.Inner)) {
        System.exit(2);
      }
      //assert that the object's class loader is the system class loader (with a different name)
      Assert.assertEquals(ClassLoader.getSystemClassLoader(), obj.getClass().getClassLoader());

      obj = sharedMap.get("3");
      if (!(obj instanceof StandardClasspathDummyClass)) {
        System.exit(3);
      }

      //assert that the object's class loader is the system class loader
      Assert.assertEquals(ClassLoader.getSystemClassLoader(), obj.getClass().getClassLoader());
    }
  }


  public static class AppInnerClass {
    //
  }
}
