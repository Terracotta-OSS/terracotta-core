/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

import java.util.List;
import java.util.Map;

public final class ActiveServerTransactionFactory implements ServerTransactionFactory {

  public ServerTransaction createServerTransaction(TxnBatchID batchID, TransactionID txnID, SequenceID sequenceID,
                                                   LockID[] locks, NodeID source, List dnas,
                                                   ObjectStringSerializer serializer, Map newRoots, TxnType txnType,
                                                   List notifies, DmiDescriptor[] dmis, MetaDataReader[] readers,
                                                   int numApplicationTxn, long[] highWaterMarks) {
    return new ServerTransactionImpl(batchID, txnID, sequenceID, locks, source, dnas, serializer, newRoots, txnType,
                                     notifies, dmis, readers, numApplicationTxn, highWaterMarks);
  }

}
