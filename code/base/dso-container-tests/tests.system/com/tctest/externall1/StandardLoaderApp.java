/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.externall1;

import com.tctest.webapp.servlets.StandardLoaderServlet;

import java.util.HashMap;
import java.util.Map;

public class StandardLoaderApp {

  private final Map sharedMap = new HashMap();

  public static void main(String[] args) {
    new StandardLoaderApp().doTest();
  }

  private void doTest() {
    synchronized (sharedMap) {
      Object obj = sharedMap.get("1");
      if (obj instanceof StandardLoaderServlet.Inner) {
        System.out.println("OK");
      } else {
        System.exit(1);
      }
    }
  }

}
