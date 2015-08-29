/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public interface GroupEventsListener {

  public void nodeJoined(NodeID nodeID);

  public void nodeLeft(NodeID nodeID);
  
}
