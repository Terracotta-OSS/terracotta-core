/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.session;

public class NullSessionManager implements SessionManager, SessionProvider {

  public SessionID getSessionID() {
    return SessionID.NULL_ID;
  }

  public void newSession() {
    return;
  }

  public boolean isCurrentSession(SessionID sessionID) {
    return true;
  }

}
