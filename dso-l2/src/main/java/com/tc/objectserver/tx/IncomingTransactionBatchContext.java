/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.ServerTransactionID;

import java.util.List;
import java.util.Set;

public class IncomingTransactionBatchContext implements TransactionBatchContext {

  private final NodeID                   nodeID;
  private final List<ServerTransaction>  txns;
  private final Set<ObjectID>            newObjectIDs;
  private final TransactionBatchReader   reader;
  private final TCByteBuffer[]           buffers;
  private final Set<ServerTransactionID> txnIDs;

  public IncomingTransactionBatchContext(final NodeID nodeID, final Set<ServerTransactionID> txnIDs,
                                         final TransactionBatchReader reader, final List<ServerTransaction> txns,
                                         final Set<ObjectID> newObjectIDs) {
    this(nodeID, txnIDs, reader, txns, newObjectIDs, reader.getBackingBuffers());
  }

  public IncomingTransactionBatchContext(final NodeID nodeID, final Set<ServerTransactionID> txnIDs,
                                         final TransactionBatchReader reader, final List<ServerTransaction> txns,
                                         final Set<ObjectID> newObjectIDs, final TCByteBuffer buffers[]) {
    this.txnIDs = txnIDs;
    this.buffers = buffers;
    this.nodeID = nodeID;
    this.reader = reader;
    this.txns = txns;
    this.newObjectIDs = newObjectIDs;
  }

  @Override
  public Set<ObjectID> getNewObjectIDs() {
    return this.newObjectIDs;
  }

  @Override
  public TransactionBatchReader getTransactionBatchReader() {
    return this.reader;
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    return this.reader.getSerializer();
  }

  @Override
  public NodeID getSourceNodeID() {
    return this.nodeID;
  }

  @Override
  public int getNumTxns() {
    return this.txns.size();
  }

  @Override
  public List<ServerTransaction> getTransactions() {
    return this.txns;
  }

  @Override
  public TCByteBuffer[] getBackingBuffers() {
    return this.buffers;
  }

  @Override
  public Set<ServerTransactionID> getTransactionIDs() {
    return this.txnIDs;
  }
}
