/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

public class InvalidatorAttributeListener implements HttpSessionAttributeListener {

  public InvalidatorAttributeListener() {
    System.err.println("### AttributeListener() is here!!!");
  }

  public void attributeAdded(HttpSessionBindingEvent httpsessionbindingevent) {
    InvalidatorServlet.incrementCallCount("AttributeListener.attributeAdded");
    System.err.println("### AttributeListener.attributeAdded() is here!!!");
  }

  public void attributeRemoved(HttpSessionBindingEvent httpsessionbindingevent) {
    System.err.println("### AttributeListener.attributeRemoved() is here!!!");
    InvalidatorServlet.incrementCallCount("AttributeListener.attributeRemoved");
  }

  public void attributeReplaced(HttpSessionBindingEvent httpsessionbindingevent) {
    System.err.println("### AttributeListener.attributeReplaced() is here!!!");
    InvalidatorServlet.incrementCallCount("AttributeListener.attributeReplaced");
  }
}
