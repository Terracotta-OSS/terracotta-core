/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.session;

import com.tc.net.NodeID;

public interface SessionProvider {
  public void initProvider(NodeID nid);
  
  public SessionID getSessionID(NodeID nid);
  
  public SessionID nextSessionID(NodeID nid);

  public void resetSessionProvider();
}
