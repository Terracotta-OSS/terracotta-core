/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpServletRequest;

public interface TerracottaRequest extends HttpServletRequest {

  String encodeRedirectURL(String url);

  String encodeURL(String url);

  Session getTerracottaSession(boolean createNew);

  boolean isSessionOwner();

  boolean isForwarded();

  long getRequestStartMillis();

  void setSessionManager(TerracottaSessionManager sessionManager);

}
