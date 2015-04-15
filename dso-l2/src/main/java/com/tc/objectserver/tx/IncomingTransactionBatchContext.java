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
