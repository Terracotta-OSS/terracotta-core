/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.groups.NodeID;

public interface L2ObjectStateListener {

  public void missingObjectsFor(NodeID nodeID, int missingObjects);

}
