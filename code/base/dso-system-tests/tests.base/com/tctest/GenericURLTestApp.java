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
import java.util.ArrayList;
import java.util.List;

public class GenericURLTestApp extends GenericTestApp {
  public final static String URL_SPEC1 = "https://www.terracotta.org/path?param1=val1&param2=val2;test#reference";
  public final static String URL_SPEC2 = "http://www.terracottatech.com:8081";
  public final static String URL_SPEC3 = "http://user:pass@www.apple.com#ref";
  
  public GenericURLTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, URL.class);
  }

  protected Object getTestObject(String testName) {
    List list = (List) sharedMap.get("list");
    return list.iterator();
  }

  protected void setupTestObject(String testName) {
    List list = new ArrayList();
    list.add(createURL(URL_SPEC1));
    list.add(createURL(URL_SPEC2));
    list.add(createURL(URL_SPEC3));
    
    sharedMap.put("list", list);
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
      Assert.assertTrue(createURL(URL_SPEC1).equals(url)
                        || createURL(URL_SPEC2).equals(url)
                        || createURL(URL_SPEC3).equals(url));
    } else {
      synchronized (url) {
        System.out.println("URL : "+url);
      }
    }
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