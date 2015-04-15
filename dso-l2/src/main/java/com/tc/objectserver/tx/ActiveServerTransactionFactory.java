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

import com.tc.net.NodeID;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.SequenceID;

import java.util.List;
import java.util.Map;

public final class ActiveServerTransactionFactory implements ServerTransactionFactory {

  @Override
  public ServerTransaction createServerTransaction(TxnBatchID batchID, TransactionID txnID, SequenceID sequenceID,
                                                   boolean isEviction,
                                                   LockID[] locks, NodeID source, List dnas,
                                                   ObjectStringSerializer serializer, Map newRoots, TxnType txnType,
                                                   List notifies, MetaDataReader[] readers,
                                                   int numApplicationTxn, long[] highWaterMarks) {
    return new ServerTransactionImpl(batchID, txnID, sequenceID, locks, source, dnas, serializer, newRoots, txnType,
                                     notifies, readers, numApplicationTxn, highWaterMarks);
  }

}
