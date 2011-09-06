/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.session;

import com.tc.net.NodeID;

public interface SessionManager extends SessionProvider {

  public boolean isCurrentSession(NodeID nid, SessionID sessionID);

  /**
   * Tells the session manager to start a new session.
   */
  public void newSession(NodeID nid);
}
