/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

public class NullSessionManager implements SessionManager {

  @Override
  public SessionID getSessionID() {
    return SessionID.NULL_ID;
  }

  @Override
  public SessionID nextSessionID() {
    return SessionID.NULL_ID;
  }

  @Override
  public void newSession() {
    return;
  }

  @Override
  public boolean isCurrentSession(SessionID sessionID) {
    return true;
  }

  @Override
  public void initProvider() {
    return;
  }

  @Override
  public void resetSessionProvider() {
    return;
  }

}
