/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc;

import java.util.Arrays;

public class WebAppComparable implements Comparable {
  private WebApp m_webApp;
    
  WebAppComparable(WebApp webApp) {
    m_webApp = webApp;
  }
    
  public WebApp element() {
    return m_webApp;
  }

  public int compareTo(Object o) {
    WebAppComparable other     = (WebAppComparable)o;
    WebApp           otherType = other.element();
    String           otherName = otherType.getName();

    return m_webApp.getName().compareTo(otherName);
  }

  public static WebApp[] sort(WebApp[] webApps) {
    int                count = webApps.length;
    WebAppComparable[] tmp = new WebAppComparable[count];
    
    for(int i = 0; i < count; i++) {
      tmp[i] = new WebAppComparable(webApps[i]);
    }
    Arrays.sort(tmp);
    for(int i = 0; i < count; i++) {
      webApps[i] = tmp[i].element();
    }

    return webApps;
  }
}
