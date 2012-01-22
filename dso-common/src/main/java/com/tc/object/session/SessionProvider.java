/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.session;

import com.tc.net.NodeID;

public interface SessionProvider {
  public void initProvider(NodeID nid);
  
  public SessionID getSessionID(NodeID nid);
  
  public SessionID nextSessionID(NodeID nid);

  public void resetSessionProvider();
}
