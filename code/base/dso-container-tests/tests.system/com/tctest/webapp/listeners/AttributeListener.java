/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.ListenerReportingServlet;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

public class AttributeListener implements HttpSessionAttributeListener {

  public AttributeListener() {
    System.err.println("### AttributeListener() is here!!!");
  }

  public void attributeAdded(HttpSessionBindingEvent httpsessionbindingevent) {
    ListenerReportingServlet.incrementCallCount("AttributeListener.attributeAdded");
    System.err.println("### AttributeListener.attributeAdded() is here!!!");
  }

  public void attributeRemoved(HttpSessionBindingEvent httpsessionbindingevent) {
    System.err.println("### AttributeListener.attributeRemoved() is here!!!");
    ListenerReportingServlet.incrementCallCount("AttributeListener.attributeRemoved");
  }

  public void attributeReplaced(HttpSessionBindingEvent httpsessionbindingevent) {
    System.err.println("### AttributeListener.attributeReplaced() is here!!!");
    ListenerReportingServlet.incrementCallCount("AttributeListener.attributeReplaced");
  }
}
