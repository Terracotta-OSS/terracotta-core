/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.NodeID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrunedServerTransaction implements ServerTransaction {

  private static final long[]     EMPTY_LONG_ARRAY = new long[0];

  private final List              prunedChanges;
  private final ServerTransaction orgTxn;
  private final ObjectIDSet       oids;
  private final ObjectIDSet       newOids;

  public PrunedServerTransaction(final List prunedChanges, final ServerTransaction st, final ObjectIDSet oids,
                                 final ObjectIDSet newOids) {
    this.prunedChanges = prunedChanges;
    this.orgTxn = st;
    this.oids = oids;
    this.newOids = newOids;
  }

  public Collection getNotifies() {
    return this.orgTxn.getNotifies();
  }

  public TxnBatchID getBatchID() {
    return this.orgTxn.getBatchID();
  }

  public List getChanges() {
    return this.prunedChanges;
  }

  public NodeID getSourceID() {
    return this.orgTxn.getSourceID();
  }

  public DmiDescriptor[] getDmiDescriptors() {
    return this.orgTxn.getDmiDescriptors();
  }
  
  public MetaDataReader[] getMetaDataReaders() {
    return this.orgTxn.getMetaDataReaders();
  }

  public LockID[] getLockIDs() {
    return this.orgTxn.getLockIDs();
  }

  public ObjectIDSet getNewObjectIDs() {
    return this.newOids;
  }

  public Map getNewRoots() {
    return this.orgTxn.getNewRoots();
  }

  public ObjectIDSet getObjectIDs() {
    return this.oids;
  }

  public ObjectStringSerializer getSerializer() {
    return this.orgTxn.getSerializer();
  }

  public ServerTransactionID getServerTransactionID() {
    return this.orgTxn.getServerTransactionID();
  }

  public TransactionID getTransactionID() {
    return this.orgTxn.getTransactionID();
  }

  public TxnType getTransactionType() {
    return this.orgTxn.getTransactionType();
  }

  public SequenceID getClientSequenceID() {
    return this.orgTxn.getClientSequenceID();
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return this.orgTxn.getGlobalTransactionID();
  }

  public boolean isActiveTxn() {
    return this.orgTxn.isActiveTxn();
  }

  public int getNumApplicationTxn() {
    return this.orgTxn.getNumApplicationTxn();
  }

  public void setGlobalTransactionID(final GlobalTransactionID gid) {
    throw new UnsupportedOperationException();
  }

  public long[] getHighWaterMarks() {
    return EMPTY_LONG_ARRAY;
  }

}
