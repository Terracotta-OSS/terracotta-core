/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.object.ServerMapRequestType;

public interface ServerMapMessageFactory {

  public ServerMapRequestMessage newServerMapRequestMessage(NodeID nodeID, ServerMapRequestType type);

}
