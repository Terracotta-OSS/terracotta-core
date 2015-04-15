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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ServerEvictionTransactionImpl extends ServerTransactionImpl {

  public ServerEvictionTransactionImpl(TxnBatchID batchID, TransactionID txID, SequenceID sequenceID, LockID[] lockIDs,
                                       NodeID source, List dnas, ObjectStringSerializer serializer, Map newRoots,
                                       TxnType transactionType, Collection notifies,
                                       MetaDataReader[] metaDataReaders, int numApplicationTxn, long[] highWaterMarks) {
    super(batchID, txID, sequenceID, lockIDs, source, dnas, serializer, newRoots, transactionType, notifies,
          metaDataReaders, numApplicationTxn, highWaterMarks);
  }

  @Override
  public boolean isEviction() {
    return true;
  }

}
