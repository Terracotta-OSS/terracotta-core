/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpServletRequest;

public interface TerracottaRequest extends HttpServletRequest {

  String encodeRedirectURL(String url);

  String encodeURL(String url);

  Session getSessionIfAny();

  boolean isUnlockSesssionId();

  long getRequestStartMillis();

  void setSessionManager(TerracottaSessionManager sessionManager);

}
