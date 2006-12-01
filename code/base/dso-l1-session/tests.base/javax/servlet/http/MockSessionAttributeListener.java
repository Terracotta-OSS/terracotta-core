/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package javax.servlet.http;

public class MockSessionAttributeListener implements HttpSessionAttributeListener {

  private String                  lastMethod;
  private HttpSessionBindingEvent lastEvent;

  public void attributeAdded(HttpSessionBindingEvent e) {
    this.lastMethod = "attributeAdded";
    this.lastEvent = e;
  }

  public void attributeRemoved(HttpSessionBindingEvent e) {
    this.lastMethod = "attributeRemoved";
    this.lastEvent = e;
  }

  public void attributeReplaced(HttpSessionBindingEvent e) {
    this.lastMethod = "attributeReplaced";
    this.lastEvent = e;
  }

  public void clear() {
    this.lastMethod = null;
    this.lastEvent = null;
  }

  public HttpSessionBindingEvent getLastEvent() {
    return lastEvent;
  }

  public String getLastMethod() {
    return lastMethod;
  }
}
