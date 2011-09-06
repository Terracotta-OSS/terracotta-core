/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.NodeID;

public interface GroupMessageListener {
  
  public void messageReceived(NodeID fromNode, GroupMessage msg);

}
