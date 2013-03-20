/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

public class ResentServerTransaction implements ServerTransaction {

  private final ServerTransaction orgTxn;

  public ResentServerTransaction(ServerTransaction wrapped) {
    orgTxn = wrapped;
  }

  @Override
  public void setGlobalTransactionID(GlobalTransactionID gid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID() {
    return orgTxn.getGlobalTransactionID();
  }

  @Override
  public SequenceID getClientSequenceID() {
    return orgTxn.getClientSequenceID();
  }

  @Override
  public TxnBatchID getBatchID() {
    return orgTxn.getBatchID();
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    return orgTxn.getSerializer();
  }

  @Override
  public LockID[] getLockIDs() {
    return orgTxn.getLockIDs();
  }

  @Override
  public NodeID getSourceID() {
    return orgTxn.getSourceID();
  }

  @Override
  public TransactionID getTransactionID() {
    return orgTxn.getTransactionID();
  }

  @Override
  public ServerTransactionID getServerTransactionID() {
    return orgTxn.getServerTransactionID();
  }

  @Override
  public List getChanges() {
    return orgTxn.getChanges();
  }

  @Override
  public Map getNewRoots() {
    return orgTxn.getNewRoots();
  }

  @Override
  public TxnType getTransactionType() {
    return orgTxn.getTransactionType();
  }

  @Override
  public ObjectIDSet getObjectIDs() {
    return orgTxn.getObjectIDs();
  }

  @Override
  public ObjectIDSet getNewObjectIDs() {
    return orgTxn.getNewObjectIDs();
  }

  @Override
  public Collection getNotifies() {
    return orgTxn.getNotifies();
  }

  @Override
  public DmiDescriptor[] getDmiDescriptors() {
    return orgTxn.getDmiDescriptors();
  }

  @Override
  public MetaDataReader[] getMetaDataReaders() {
    return orgTxn.getMetaDataReaders();
  }

  @Override
  public boolean isActiveTxn() {
    return orgTxn.isActiveTxn();
  }

  @Override
  public boolean isResent() {
    return true;
  }

  @Override
  public int getNumApplicationTxn() {
    return orgTxn.getNumApplicationTxn();
  }

  @Override
  public long[] getHighWaterMarks() {
    return orgTxn.getHighWaterMarks();
  }

  @Override
  public boolean isSearchEnabled() {
    return orgTxn.isSearchEnabled();
  }

  @Override
  public boolean isEviction() {
    return false;
  }

}
