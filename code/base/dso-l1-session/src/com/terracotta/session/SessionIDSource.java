/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

public class SessionIDSource {

  public static final SessionIDSource NONE = new SessionIDSource("none");
  public static final SessionIDSource COOKIE  = new SessionIDSource("cookie");
  public static final SessionIDSource URL     = new SessionIDSource("url");

  private final String                source;

  private SessionIDSource(String source) {
    this.source = source;
  }

  public String toString() {
    return source;
  }

  public boolean isCookie() {
    return this == COOKIE;
  }

  public boolean isURL() {
    return this == URL;
  }

}
