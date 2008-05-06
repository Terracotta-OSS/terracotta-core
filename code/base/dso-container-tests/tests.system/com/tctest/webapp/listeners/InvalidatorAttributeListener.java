/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.ListenerReportingServlet;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

public class InvalidatorAttributeListener implements HttpSessionAttributeListener {

  public InvalidatorAttributeListener() {
    System.err.println("### AttributeListener() is here!!!");
  }

  public void attributeAdded(HttpSessionBindingEvent e) {
    System.err.println("### AttributeListener.attributeAdded("+e.getName()+") is here!!!");
    ListenerReportingServlet.incrementCallCount("AttributeListener.attributeAdded");
  }

  public void attributeRemoved(HttpSessionBindingEvent e) {
    System.err.println("### AttributeListener.attributeRemoved("+e.getName()+") is here!!!");
    ListenerReportingServlet.incrementCallCount("AttributeListener.attributeRemoved");
  }

  public void attributeReplaced(HttpSessionBindingEvent e) {
    System.err.println("### AttributeListener.attributeReplaced("+e.getName()+") is here!!!");
    ListenerReportingServlet.incrementCallCount("AttributeListener.attributeReplaced");
  }
}
