/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public interface GroupMessageListener<M extends GroupMessage> {
  
  public void messageReceived(NodeID fromNode, M msg);

}
