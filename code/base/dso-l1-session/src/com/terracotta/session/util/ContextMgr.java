/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.terracotta.session.util;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionContext;

public interface ContextMgr {
  String getAppName();

  HttpSessionContext getSessionContext();

  ServletContext getServletContext();

}
