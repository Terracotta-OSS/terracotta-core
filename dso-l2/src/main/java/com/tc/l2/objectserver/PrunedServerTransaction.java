/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.tc.net.NodeID;
import com.tc.object.dna.api.DNAInternal;
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

  private final List<DNAInternal> prunedChanges;
  private final ServerTransaction orgTxn;
  private final ObjectIDSet       oids;
  private final ObjectIDSet       newOids;

  public PrunedServerTransaction(final List<DNAInternal> prunedChanges, final ServerTransaction st,
                                 final ObjectIDSet oids,
                                 final ObjectIDSet newOids) {
    this.prunedChanges = prunedChanges;
    this.orgTxn = st;
    this.oids = oids;
    this.newOids = newOids;
  }

  @Override
  public Collection getNotifies() {
    return this.orgTxn.getNotifies();
  }

  @Override
  public TxnBatchID getBatchID() {
    return this.orgTxn.getBatchID();
  }

  @Override
  public List getChanges() {
    return this.prunedChanges;
  }

  @Override
  public NodeID getSourceID() {
    return this.orgTxn.getSourceID();
  }

  @Override
  public MetaDataReader[] getMetaDataReaders() {
    // Use metadata from changes (if any), NOT from original txn! Reason being that txn carries metadata for containing maps,
    // pruned changes in this case are applied as they are relayed, while metadata must be saved off with pending changes to await
    // object sync txns
    return Iterables.toArray(Iterables.transform(prunedChanges, new Function<DNAInternal, MetaDataReader>() {

      @Override
      public MetaDataReader apply(DNAInternal from) {
        return from.getMetaDataReader();
      }

    }), MetaDataReader.class);

  }

  @Override
  public LockID[] getLockIDs() {
    return this.orgTxn.getLockIDs();
  }

  @Override
  public ObjectIDSet getNewObjectIDs() {
    return this.newOids;
  }

  @Override
  public Map getNewRoots() {
    return this.orgTxn.getNewRoots();
  }

  @Override
  public ObjectIDSet getObjectIDs() {
    return this.oids;
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    return this.orgTxn.getSerializer();
  }

  @Override
  public ServerTransactionID getServerTransactionID() {
    return this.orgTxn.getServerTransactionID();
  }

  @Override
  public TransactionID getTransactionID() {
    return this.orgTxn.getTransactionID();
  }

  @Override
  public TxnType getTransactionType() {
    return this.orgTxn.getTransactionType();
  }

  @Override
  public SequenceID getClientSequenceID() {
    return this.orgTxn.getClientSequenceID();
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID() {
    return this.orgTxn.getGlobalTransactionID();
  }

  @Override
  public boolean isActiveTxn() {
    return this.orgTxn.isActiveTxn();
  }

  @Override
  public boolean isResent() {
    return this.orgTxn.isResent();
  }

  @Override
  public int getNumApplicationTxn() {
    return this.orgTxn.getNumApplicationTxn();
  }

  @Override
  public void setGlobalTransactionID(final GlobalTransactionID gid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long[] getHighWaterMarks() {
    return EMPTY_LONG_ARRAY;
  }

  @Override
  public boolean isSearchEnabled() {
    return this.orgTxn.isSearchEnabled();
  }

  @Override
  public boolean isEviction() {
    return this.orgTxn.isEviction();
  }

}
