/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.locks.LockID;
import com.tc.object.locks.Notify;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.SequenceID;

import java.util.List;
import java.util.Map;

public interface ServerTransactionFactory {

  public ServerTransaction createServerTransaction(TxnBatchID batchID, TransactionID txnID, SequenceID sequenceID,
                                                   boolean isEviction,
                                                   LockID[] locks, NodeID source, List<? extends DNA> dnas,
                                                   ObjectStringSerializer serializer, Map<String, ObjectID> newRoots, TxnType txnType,
                                                   List<Notify> notifies,
                                                   int numApplictionTxn, long[] highwaterMarks);

}
