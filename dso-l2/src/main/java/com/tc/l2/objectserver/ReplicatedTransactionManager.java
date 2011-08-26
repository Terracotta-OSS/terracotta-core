/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.l2.state.StateChangeListener;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;

import java.util.Set;

public interface ReplicatedTransactionManager extends StateChangeListener, PassiveTransactionManager {

  public void publishResetRequest(NodeID nodeID) throws GroupException;

  public void init(Set knownObjectIDs);

}
