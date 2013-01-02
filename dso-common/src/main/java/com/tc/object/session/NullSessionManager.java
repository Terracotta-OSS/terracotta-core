/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

import com.tc.net.NodeID;

public class NullSessionManager implements SessionManager, SessionProvider {

  @Override
  public SessionID getSessionID(NodeID nid) {
    return SessionID.NULL_ID;
  }

  @Override
  public SessionID nextSessionID(NodeID nid) {
    return SessionID.NULL_ID;
  }

  @Override
  public void newSession(NodeID nid) {
    return;
  }

  @Override
  public boolean isCurrentSession(NodeID nid, SessionID sessionID) {
    return true;
  }

  @Override
  public void initProvider(NodeID nid) {
    return;
  }

  @Override
  public void resetSessionProvider() {
    return;
  }

}
