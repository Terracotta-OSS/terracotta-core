/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.ListenerReportingServlet;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public final class InvalidatorBindingListener implements HttpSessionBindingListener {

  private final String key;
  
  public InvalidatorBindingListener() {
    key = null;
  }

  public InvalidatorBindingListener(String key) {
    System.err.println("### BindingListener is here!!! key = " + key);
    this.key = key;
  }

  public void valueBound(HttpSessionBindingEvent e) {
    System.err.println("### BindingListener.valueBound: " + e.getValue());
    ListenerReportingServlet.incrementCallCount("BindingListener.valueBound");
  }

  public void valueUnbound(HttpSessionBindingEvent e) {
    System.err.println("### BindingListener.valueUnbound: " + e.getValue());
    ListenerReportingServlet.incrementCallCount("BindingListener.valueUnbound");
  }

  public String toString() {
    return key;
  }
}