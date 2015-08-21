/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

public interface SessionManager extends SessionProvider {

  public boolean isCurrentSession(SessionID sessionID);

  /**
   * Tells the session manager to start a new session.
   */
  public void newSession();
}
