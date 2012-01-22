/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
