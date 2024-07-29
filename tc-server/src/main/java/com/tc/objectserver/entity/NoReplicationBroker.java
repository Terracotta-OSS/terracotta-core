/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.entity;

import com.tc.l2.msg.SyncReplicationActivity;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;
import java.util.Collections;
import java.util.Set;


/**
 * Stubbed implementation which provides no replication.
 */
public class NoReplicationBroker implements PassiveReplicationBroker {
    
  public static final ActivePassiveAckWaiter NOOP_WAITER = new ActivePassiveAckWaiter(Collections.emptyMap(), Collections.emptySet(), null);

  @Override
  public Set<SessionID> passives() {
    return Collections.emptySet();
  }

  @Override
  public ActivePassiveAckWaiter replicateActivity(SyncReplicationActivity activity, Set<SessionID> passives) {
    return NOOP_WAITER;
  }

  @Override
  public void zapAndWait(NodeID node) {
    //  do nothing
  }
}
