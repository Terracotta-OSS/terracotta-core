/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

public class ServerTransactionBatchContext implements TransactionBatchContext {

  private final List<ServerTransaction>  txns;
  private final NodeID                   nodeID;
  private final Set<ServerTransactionID> txnIDs = new HashSet<ServerTransactionID>();
  private TCByteBuffer[]                 buffers;
  private final ObjectStringSerializer   serializer;
  private final TxnBatchID               batchID;

  public ServerTransactionBatchContext(final NodeID nodeID, final ServerTransaction txn,
                                       final ObjectStringSerializer serializer) {
    this.nodeID = nodeID;
    this.serializer = serializer;
    this.batchID = txn.getBatchID();
    this.txns = Collections.singletonList(txn);
    this.txnIDs.add(txn.getServerTransactionID());
  }

  @Override
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

  @Override
  public Set<ObjectID> getNewObjectIDs() {
    return Collections.EMPTY_SET;
  }

  @Override
  public int getNumTxns() {
    return 1;
  }

  @Override
  public NodeID getSourceNodeID() {
    return this.nodeID;
  }

  @Override
  public TransactionBatchReader getTransactionBatchReader() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<ServerTransactionID> getTransactionIDs() {
    return this.txnIDs;
  }

  @Override
  public List<ServerTransaction> getTransactions() {
    return this.txns;
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    return this.serializer;
  }
}
