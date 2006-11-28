/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.session;

public interface SessionManager {
  
  public boolean isCurrentSession(SessionID sessionID);

  /**
   * Tells the session manager to start a new session.
   */
  public void newSession();
}
