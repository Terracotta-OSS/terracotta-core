/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.ListenerReportingServlet;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public class BindingListener implements HttpSessionBindingListener {

  private final String key;
  
  // this default constructor is needed for deployment
  public BindingListener() {
    key = null;
  }

  public BindingListener(String key) {
    System.err.println("### BindingListener is here!!! key = " + key);
    this.key = key;
  }

  public void valueBound(HttpSessionBindingEvent e) {
    System.err.println("### BindingListener.valueBound");
    // the value being bound must not be in session yet...
    ListenerReportingServlet.incrementCallCount("BindingListener.valueBound");
  }

  public void valueUnbound(HttpSessionBindingEvent e) {
    System.err.println("### BindingListener.valueUnbound");
    // the value being unbound must not be in session already...
    ListenerReportingServlet.incrementCallCount("BindingListener.valueUnbound");
  }

  public String toString() {
    return key;
  }
}
