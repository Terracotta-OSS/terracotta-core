/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchContext;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.ServerTransactionBatchWriter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerMapEvictionTransactionBatchContext implements TransactionBatchContext {

  private final List<ServerTransaction>  txns;
  private final NodeID                   nodeID;
  private final Set<ServerTransactionID> txnIDs = new HashSet();
  private TCByteBuffer[]                 buffers;
  private final ObjectStringSerializer   serializer;
  private final TxnBatchID               batchID;

  public ServerMapEvictionTransactionBatchContext(final NodeID nodeID, final ServerTransaction txn,
                                                  final ObjectStringSerializer serializer) {
    this.nodeID = nodeID;
    this.serializer = serializer;
    this.batchID = txn.getBatchID();
    this.txns = Collections.singletonList(txn);
    this.txnIDs.add(txn.getServerTransactionID());
  }

  public TCByteBuffer[] getBackingBuffers() {
    if (this.buffers == null) {
      this.buffers = constructTransactionBatchBuffers();
    }
    return this.buffers;
  }

  private TCByteBuffer[] constructTransactionBatchBuffers() {
    final ServerTransactionBatchWriter txnWriter = new ServerTransactionBatchWriter(this.batchID, this.serializer);
    try {
      return txnWriter.writeTransactionBatch(this.txns);
    } catch (final Exception e) {
      throw new AssertionError(e);
    }
  }

  public Set<ObjectID> getNewObjectIDs() {
    return Collections.EMPTY_SET;
  }

  public int getNumTxns() {
    return 1;
  }

  public NodeID getSourceNodeID() {
    return this.nodeID;
  }

  public TransactionBatchReader getTransactionBatchReader() {
    throw new UnsupportedOperationException();
  }

  public Set<ServerTransactionID> getTransactionIDs() {
    return this.txnIDs;
  }

  public List<ServerTransaction> getTransactions() {
    return this.txns;
  }

  public ObjectStringSerializer getSerializer() {
    return this.serializer;
  }
}
