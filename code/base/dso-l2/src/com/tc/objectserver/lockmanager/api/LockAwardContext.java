/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.api;

import com.tc.net.groups.NodeID;
import com.tc.object.lockmanager.api.LockID;

public interface LockAwardContext {

  public NodeID getNodeID();

  public LockID getLockID();
  
  public long getTimeout();

}
