/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import java.util.Enumeration;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

public class DefaultSessionContext implements HttpSessionContext {

  public final static HttpSessionContext theInstance = new DefaultSessionContext();

  private DefaultSessionContext() {
    // prevent construction
  }

  private final static Enumeration elements = new Enumeration() {
                                              public boolean hasMoreElements() {
                                                return false;
                                              }

                                              public Object nextElement() {
                                                return null;
                                              }
                                            };

  public Enumeration getIds() {
    return elements;
  }

  public HttpSession getSession(String arg0) {
    return null;
  }

}
