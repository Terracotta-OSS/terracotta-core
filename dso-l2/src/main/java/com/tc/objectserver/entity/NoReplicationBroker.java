/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.entity;

import com.tc.l2.msg.ReplicationMessage;
import com.tc.net.NodeID;
import com.tc.util.Assert;
import java.util.Collections;
import java.util.Set;


/**
 * Stubbed implementation which provides no replication.
 */
public class NoReplicationBroker implements PassiveReplicationBroker {
  
  private boolean isActive = false;
  
  public static final ActivePassiveAckWaiter NOOP_WAITER = new ActivePassiveAckWaiter(Collections.emptySet(), null);

  @Override
  public void enterActiveState() {
// only happens once
    Assert.assertFalse(isActive);
    isActive = true;
  }

  @Override
  public Set<NodeID> passives() {
    return Collections.emptySet();
  }

  @Override
  public ActivePassiveAckWaiter replicateMessage(ReplicationMessage msg, Set<NodeID> passives) {
    return NOOP_WAITER;
  }

  @Override
  public void zapAndWait(NodeID node) {
    //  do nothing
  }
  
  
}
