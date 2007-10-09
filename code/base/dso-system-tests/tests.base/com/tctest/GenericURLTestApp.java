/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.net.MalformedURLException;
import java.net.URL;

public class GenericURLTestApp extends GenericTestApp {
  public final static String URL_SPEC = "https://www.terracotta.org/path?param1=val1&param2=val2;test";
  
  public GenericURLTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, URL.class);
  }

  protected Object getTestObject(String testName) {
    return sharedMap.get("url");
  }

  protected void setupTestObject(String testName) {
    sharedMap.put("url", createURL(URL_SPEC));
  }
  
  private URL createURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  void testSharedURL(URL url, boolean validate) {
    if (validate) {
      assertEqualURL(createURL(URL_SPEC), url);
    } else {
      synchronized (url) {
        System.out.println(url);
      }
    }
  }
  
  private void assertEqualURL(URL expect, URL actual) {
    Assert.assertNotNull(expect);
    Assert.assertNotNull(actual);
    Assert.assertEquals(expect, actual);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = GenericURLTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    String writeAllowedMethodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(writeAllowedMethodExpression);
    String readOnlyMethodExpression = "* " + testClass + "*.*ReadOnly*(..)";
    config.addReadAutolock(readOnlyMethodExpression);
  }
}