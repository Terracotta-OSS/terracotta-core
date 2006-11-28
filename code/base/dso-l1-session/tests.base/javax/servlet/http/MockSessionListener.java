/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package javax.servlet.http;

public class MockSessionListener implements HttpSessionListener {

  private String           lastMethod;
  private HttpSessionEvent lastEvent;

  public void sessionCreated(HttpSessionEvent e) {
    this.lastMethod = "sessionCreated";
    this.lastEvent = e;
  }

  public void sessionDestroyed(HttpSessionEvent e) {
    this.lastMethod = "sessionDestroyed";
    this.lastEvent = e;
  }

  public void clear() {
    this.lastEvent = null;
    this.lastMethod = null;
  }

  public HttpSessionEvent getLastEvent() {
    return lastEvent;
  }

  public String getLastMethod() {
    return lastMethod;
  }

}
