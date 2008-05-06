/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.ListenerReportingServlet;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

/**
 * Tests if the value being bound is in the session already.  It should not be but
 * in Jetty it is.
 */
public final class BindingSequenceListener implements HttpSessionBindingListener {

  private final String key;

  public BindingSequenceListener() {
    key = null;
  }

  public BindingSequenceListener(String key) {
    System.err.println("### BindingListener is here!!! key = " + key);
    this.key = key;
  }

  public void valueBound(HttpSessionBindingEvent e) {
    System.err.println("### BindSequenceListener.valueBound(" + e.getName() + ")");
    // the value being bound must not be in session yet...
    Object o = e.getSession().getAttribute(e.getName());
    if (o == null) {
      ListenerReportingServlet.incrementCallCount("BindSequenceListener.valueBound");
    } else {
      System.err.println("### Event sequence violated: the value being bound must not be in session yet");
    }
  }

  public void valueUnbound(HttpSessionBindingEvent e) {
    System.err.println("### BindSequenceListener.valueUnbound(" + e.getName() + ")");
    ListenerReportingServlet.incrementCallCount("BindSequenceListener.valueUnbound");
  }

  public String toString() {
    return key;
  }
}