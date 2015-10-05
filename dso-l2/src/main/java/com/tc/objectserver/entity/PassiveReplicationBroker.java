/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.entity;

import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ServerEntityAction;
import java.util.concurrent.Future;

public interface PassiveReplicationBroker {
    Future<Void> replicateMessage(EntityDescriptor id, long version, NodeID src, int concurrency, 
        ServerEntityAction type, TransactionID tid, TransactionID oldest, byte[] payload);
    boolean isActive();
    void setActive(boolean active);
}
