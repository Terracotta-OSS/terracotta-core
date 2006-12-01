/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package javax.servlet.http;

public class MockSessionBindingListener implements HttpSessionBindingListener {

  private String                  lastEventName = null;
  private HttpSessionBindingEvent lastEvent;

  public HttpSessionBindingEvent getLastEvent() {
    return lastEvent;
  }

  public String getLastEventName() {
    return lastEventName;
  }

  public void valueBound(HttpSessionBindingEvent arg0) {
    lastEventName = "valueBound";
    this.lastEvent = arg0;
  }

  public void valueUnbound(HttpSessionBindingEvent arg0) {
    lastEventName = "valueUnbound";
    this.lastEvent = arg0;
  }
  
  public void clear() {
    this.lastEvent = null;
    this.lastEventName = null;
  }

}
