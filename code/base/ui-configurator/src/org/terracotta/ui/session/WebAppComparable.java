/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.session;

import java.util.Arrays;

public class WebAppComparable implements Comparable {
  private WebApp webApp;

  WebAppComparable(WebApp webApp) {
    this.webApp = webApp;
  }

  public WebApp element() {
    return webApp;
  }

  public int compareTo(Object o) {
    WebAppComparable other = (WebAppComparable) o;
    WebApp otherType = other.element();
    String otherName = otherType.getName();

    return webApp.getName().compareTo(otherName);
  }

  public static WebApp[] sort(WebApp[] webApps) {
    int count = webApps.length;
    WebAppComparable[] tmp = new WebAppComparable[count];

    for (int i = 0; i < count; i++) {
      tmp[i] = new WebAppComparable(webApps[i]);
    }
    Arrays.sort(tmp);
    for (int i = 0; i < count; i++) {
      webApps[i] = tmp[i].element();
    }

    return webApps;
  }
}
