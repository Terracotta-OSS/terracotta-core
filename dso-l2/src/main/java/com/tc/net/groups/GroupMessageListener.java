/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;
import com.tc.net.groups.GroupMessage;

public interface GroupMessageListener<M extends GroupMessage> {
  
  public void messageReceived(NodeID fromNode, M msg);

}
