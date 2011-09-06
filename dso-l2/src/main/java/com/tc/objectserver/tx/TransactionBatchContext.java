/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.ServerTransactionID;

import java.util.List;
import java.util.Set;

public interface TransactionBatchContext extends EventContext {

  public abstract Set<ServerTransactionID> getTransactionIDs();

  public abstract Set<ObjectID> getNewObjectIDs();

  public abstract TransactionBatchReader getTransactionBatchReader();

  public abstract ObjectStringSerializer getSerializer();

  public abstract NodeID getSourceNodeID();

  public abstract int getNumTxns();

  public abstract List<ServerTransaction> getTransactions();

  public abstract TCByteBuffer[] getBackingBuffers();

}