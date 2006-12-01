/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.session;

public interface SessionManager {
  
  public boolean isCurrentSession(SessionID sessionID);

  /**
   * Tells the session manager to start a new session.
   */
  public void newSession();
}
