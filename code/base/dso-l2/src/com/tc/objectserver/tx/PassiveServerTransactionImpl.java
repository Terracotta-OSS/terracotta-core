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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PassiveServerTransactionImpl extends ServerTransactionImpl implements ServerTransaction {

  public PassiveServerTransactionImpl(TxnBatchID batchID, TransactionID txID, SequenceID sequenceID, LockID[] lockIDs,
                                      NodeID source, List dnas, ObjectStringSerializer serializer, Map newRoots,
                                      TxnType transactionType, Collection notifies, DmiDescriptor[] dmis, 
                                      MetaDataReader [] metaDataReaders, int numApplicationTxn, long[] highWaterMarks) {
    super(batchID, txID, sequenceID, lockIDs, source, dnas, serializer, newRoots, transactionType, notifies, dmis,
          metaDataReaders, numApplicationTxn, highWaterMarks);
  }

  @Override
  public DmiDescriptor[] getDmiDescriptors() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection getNotifies() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActiveTxn() {
    return false;
  }

  @Override
  public String toString() {
    return "PassiveServerTransactionImpl [ " + super.toString() + " ]";
  }
}
