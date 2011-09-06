/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;

public class GenericURLTestApp extends GenericTransparentApp {
  public final static String URL_SPEC1     = "https://www.terracotta.org/path?param1=val1&param2=val2;test#reference";
  public final static String URL_SPEC2     = "http://www.terracottatech.com:8081";
  public final static String URL_SPEC3     = "http://user:pass@www.apple.com#ref";

  public final static String URL_SPEC1_NEW = "https://dso.www.terracotta.org/path/tim?param1=val1&param2=val2;test#reference";
  public final static String URL_SPEC2_NEW = "http://dso.www.terracottatech.com:8081/tim";
  public final static String URL_SPEC3_NEW = "http://dso.www.apple.com/tim#ref";

  public GenericURLTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, URL.class);
  }

  protected Object getTestObject(String testName) {
    if ("SharedURL".equals(testName)) {
      return ((List) sharedMap.get("list1")).iterator();
    } else if ("URLWithCustomHandler".equals(testName)) { return ((List) sharedMap.get("list2")).iterator(); }

    return null;
  }

  protected void setupTestObject(String testName) {
    List list1 = new ArrayList();
    list1.add(createURL(URL_SPEC1));
    list1.add(createURL(URL_SPEC2));
    list1.add(createURL(URL_SPEC3));

    List list2 = new ArrayList();
    list2.add(createURLWithCustomHandler(URL_SPEC1));
    list2.add(createURLWithCustomHandler(URL_SPEC2));
    list2.add(createURLWithCustomHandler(URL_SPEC3));

    sharedMap.put("list1", list1);
    sharedMap.put("list2", list2);
  }

  private URL createURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private URL createURLWithCustomHandler(String url) {
    try {
      return new URL(null, url, new DummyURLStreamHandler());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  void testSharedURL(URL url, boolean validate) throws Exception {
    if (validate) {
      if (createURL(URL_SPEC1).equals(url)) {
        Assert.assertEquals(createURL(URL_SPEC1).toExternalForm(), url.toExternalForm());
      } else if (createURL(URL_SPEC2).equals(url)) {
        Assert.assertEquals(createURL(URL_SPEC2).toExternalForm(), url.toExternalForm());
      } else if (createURL(URL_SPEC3).equals(url)) {
        Assert.assertEquals(createURL(URL_SPEC3).toExternalForm(), url.toExternalForm());
      } else {
        Assert.fail();
      }
    } else {
      synchronized (url) {
        System.out.println("SharedURL : " + url);
      }
    }
  }

  void testURLWithCustomHandler(URL url, boolean validate) throws Exception {
    if (validate) {
      Assert.assertTrue(URL_SPEC1_NEW.equals(url.toExternalForm()) || URL_SPEC2_NEW.equals(url.toExternalForm())
                        || URL_SPEC3_NEW.equals(url.toExternalForm()));
    } else {
      synchronized (url) {
        url.openConnection();
        System.out.println("URLWithCustomHandler : " + url);
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

  public static class DummyURLStreamHandler extends URLStreamHandler {
    protected URLConnection openConnection(URL u) {
      setURL(u, u.getProtocol(), "dso." + u.getHost(), u.getPort(), u.getFile(), u.getRef());
      setURL(u, u.getProtocol(), u.getHost(), u.getPort(), u.getAuthority(), u.getUserInfo(), u.getPath() + "/tim", u
          .getQuery(), u.getRef());
      return null;
    }
  }
}