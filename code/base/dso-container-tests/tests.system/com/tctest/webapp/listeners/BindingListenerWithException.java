/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.InvalidatorServlet;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public class BindingListenerWithException implements HttpSessionBindingListener {
  private final String key;

  public BindingListenerWithException() {
    key = null;
  }
  
  public BindingListenerWithException(String key) {
    this.key = key;
  }

  public void valueBound(HttpSessionBindingEvent arg0) {
    InvalidatorServlet.incrementCallCount("BindingListener.valueBound");
    throw new RuntimeException("Testing Exception Delivery");
  }

  public void valueUnbound(HttpSessionBindingEvent arg0) {
    InvalidatorServlet.incrementCallCount("BindingListener.valueUnbound");
    throw new RuntimeException("Testing Exception Delivery");
  }

  public String toString() {
    return key;
  }
}
