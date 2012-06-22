/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.dmi.DmiDescriptor;
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
                                       TxnType transactionType, Collection notifies, DmiDescriptor[] dmis,
                                       MetaDataReader[] metaDataReaders, int numApplicationTxn, long[] highWaterMarks) {
    super(batchID, txID, sequenceID, lockIDs, source, dnas, serializer, newRoots, transactionType, notifies, dmis,
          metaDataReaders, numApplicationTxn, highWaterMarks);
  }

  @Override
  public boolean isEviction() {
    return true;
  }

}
