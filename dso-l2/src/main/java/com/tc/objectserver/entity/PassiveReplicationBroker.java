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
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import java.util.Set;
import java.util.concurrent.Future;

public interface PassiveReplicationBroker {
  Future<Void> replicateSync(ReplicationMessage msg, Set<NodeID> passives);
    Future<Void> replicateMessage(EntityDescriptor id, long version, NodeID src, 
        ServerEntityAction type, TransactionID tid, TransactionID oldest, byte[] payload,int concurrency);
    boolean isActive();
    void setActive(boolean active);
}
