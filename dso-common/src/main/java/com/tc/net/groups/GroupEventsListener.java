/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public interface GroupEventsListener {

  public void nodeJoined(NodeID nodeID);

  public void nodeLeft(NodeID nodeID);
  
}
