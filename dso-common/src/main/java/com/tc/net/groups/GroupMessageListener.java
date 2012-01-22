/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public interface GroupMessageListener {
  
  public void messageReceived(NodeID fromNode, GroupMessage msg);

}
