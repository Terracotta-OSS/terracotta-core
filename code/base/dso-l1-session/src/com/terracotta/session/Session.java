/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.session;

import javax.servlet.http.HttpSession;

public interface Session extends HttpSession {

  public SessionData getSessionData();

  public SessionId getSessionId();

  public boolean isValid();

  public void associateRequest(SessionRequest request);

  public void clearRequest();
}
