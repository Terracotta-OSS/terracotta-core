/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

public interface ContextMgr {
  String getAppName();

  HttpSessionContext getSessionContext();

  ServletContext getServletContext();

}
