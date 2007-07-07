/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.listeners;

import com.tctest.webapp.servlets.InvalidatorServlet;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class InvalidatorSessionListener implements HttpSessionListener {
  public InvalidatorSessionListener() {
    System.err.println("### SessionListener() is here!!!");
  }

  public void sessionCreated(HttpSessionEvent httpsessionevent) {
    System.err.println("### SessionListener.sessionCreated() is here!!!");
    InvalidatorServlet.incrementCallCount("SessionListener.sessionCreated");
  }

  public void sessionDestroyed(HttpSessionEvent httpsessionevent) {
    System.err.println("### SessionListener.sessionDestroyed() is here!!!");
    InvalidatorServlet.incrementCallCount("SessionListener.sessionDestroyed");
  }
}
