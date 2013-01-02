/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.session;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;


public class TestSessionManager implements SessionManager, SessionProvider {

  public boolean isCurrentSession = true;
  public SessionID sessionID = SessionID.NULL_ID;
  
  @Override
  public SessionID getSessionID(NodeID nid) {
    return sessionID;
  }
  
  @Override
  public SessionID nextSessionID(NodeID nid) {
    throw new ImplementMe();
  }

  @Override
  public void newSession(NodeID nid) {
    return;
  }

  @Override
  public boolean isCurrentSession(NodeID nid, SessionID theSessionID) {
    return isCurrentSession;
  }

  @Override
  public void initProvider(NodeID nid) {
    return;
  }

  @Override
  public void resetSessionProvider() {
    throw new ImplementMe();
  }

}
