/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.session;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;


public class TestSessionManager implements SessionManager, SessionProvider {

  public boolean isCurrentSession = true;
  public SessionID sessionID = SessionID.NULL_ID;
  
  public SessionID getSessionID(NodeID nid) {
    return sessionID;
  }
  
  public SessionID nextSessionID(NodeID nid) {
    throw new ImplementMe();
  }

  public void newSession(NodeID nid) {
    return;
  }

  public boolean isCurrentSession(NodeID nid, SessionID theSessionID) {
    return isCurrentSession;
  }

  public void initProvider(NodeID nid) {
    return;
  }

  public void resetSessionProvider() {
    throw new ImplementMe();
  }

}
