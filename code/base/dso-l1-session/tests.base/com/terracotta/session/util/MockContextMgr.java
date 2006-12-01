/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.tc.exception.ImplementMe;
import com.terracotta.session.util.ContextMgr;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

public class MockContextMgr implements ContextMgr {

  public ServletContext getServletContext() {
    throw new ImplementMe();
  }

  public HttpSessionContext getSessionContext() {
    throw new ImplementMe();
  }

  public String getAppName() {
    return null;
  }
}
