/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;

public interface ObjectIDBatchRequest {

  public int getBatchSize();

  public NodeID getRequestingNodeID();

}
