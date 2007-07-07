/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.InvalidatorServlet;

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
    // the value being bound must not be in session yet...
    Object o = e.getSession().getAttribute(e.getName());
    if (o == null) InvalidatorServlet.incrementCallCount("BindingListener.valueBound");
    else System.err.println("### Event sequence violated!!!");
  }

  public void valueUnbound(HttpSessionBindingEvent e) {
    System.err.println("### BindingListener.valueUnbound: " + e.getValue());
    InvalidatorServlet.incrementCallCount("BindingListener.valueUnbound");
  }

  public String toString() {
    return key;
  }
}