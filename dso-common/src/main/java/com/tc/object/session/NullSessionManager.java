/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

import com.tc.net.NodeID;

public class NullSessionManager implements SessionManager, SessionProvider {

  public SessionID getSessionID(NodeID nid) {
    return SessionID.NULL_ID;
  }

  public SessionID nextSessionID(NodeID nid) {
    return SessionID.NULL_ID;
  }

  public void newSession(NodeID nid) {
    return;
  }

  public boolean isCurrentSession(NodeID nid, SessionID sessionID) {
    return true;
  }

  public void initProvider(NodeID nid) {
    return;
  }

  public void resetSessionProvider() {
    return;
  }

}
