/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionListener implements HttpSessionListener {
  public SessionListener() {
    System.err.println("### SessionListener() is here!!!");
  }

  public void sessionCreated(HttpSessionEvent httpsessionevent) {
    System.err.println("### SessionListener.sessionCreated() is here!!!");
    ListenerReportingServlet.incrementCallCount("SessionListener.sessionCreated");
  }

  public void sessionDestroyed(HttpSessionEvent httpsessionevent) {
    testAttributeAccess(httpsessionevent.getSession());
    System.err.println("### SessionListener.sessionDestroyed() is here!!!");
    ListenerReportingServlet.incrementCallCount("SessionListener.sessionDestroyed");
  }

  private void testAttributeAccess(HttpSession session) {
    // While session destroyed event is being called, you should still be able to get
    // attributes

    String[] attrs = session.getValueNames();
    if (attrs == null || attrs.length == 0) {
      // please make at least one attribute is present
      throw new AssertionError("Attributes should be present during this phase");
    }

    for (int i = 0; i < attrs.length; i++) {
      String attr = attrs[i];
      session.getAttribute(attr);
    }
  }
}
