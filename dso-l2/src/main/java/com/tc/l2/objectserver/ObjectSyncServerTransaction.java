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
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ObjectSyncServerTransaction implements ServerTransaction {

  private static final long[]           EMPTY_LONG_ARRAY             = new long[0];

  private static final MetaDataReader[] EMPTY_META_DATA_READER_ARRAY = new MetaDataReader[] {};

  private final TransactionID           txnID;
  private final ObjectIDSet             oids;
  private final List                    changes;
  private final ServerTransactionID     serverTxnID;
  private final Map                     rootsMap;
  private final NodeID                  serverID;

  public ObjectSyncServerTransaction(final ServerTransactionID serverTransactionID, final ObjectIDSet oids,
                                     final int dnaCount, final List changes, final Map rootsMap, final NodeID serverID) {
    this.oids = oids;
    this.changes = changes;
    this.rootsMap = rootsMap;
    this.serverID = serverID;
    this.serverTxnID = serverTransactionID;
    this.txnID = serverTransactionID.getClientTransactionID();
    Assert.assertEquals(serverID, serverTransactionID.getSourceID());
    Assert.assertEquals(dnaCount, oids.size());
    Assert.assertEquals(dnaCount, changes.size());
  }

  @Override
  public Collection getNotifies() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public TxnBatchID getBatchID() {
    return TxnBatchID.NULL_BATCH_ID;
  }

  @Override
  public List getChanges() {
    return this.changes;
  }

  @Override
  public NodeID getSourceID() {
    return this.serverID;
  }

  @Override
  public SequenceID getClientSequenceID() {
    return SequenceID.NULL_ID;
  }

  @Override
  public DmiDescriptor[] getDmiDescriptors() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetaDataReader[] getMetaDataReaders() {
    // meta data is not stored in the object state so there is nothing to provide here on the object sync.
    return EMPTY_META_DATA_READER_ARRAY;
  }

  @Override
  public LockID[] getLockIDs() {
    return new LockID[0];
  }

  @Override
  public Map getNewRoots() {
    return this.rootsMap;
  }

  @Override
  public ObjectIDSet getObjectIDs() {
    return this.oids;
  }

  /*
   * All objects contained in ObjectSync Message are new Objects for the passive
   */
  @Override
  public ObjectIDSet getNewObjectIDs() {
    return this.oids;
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ServerTransactionID getServerTransactionID() {
    return this.serverTxnID;
  }

  @Override
  public TransactionID getTransactionID() {
    return this.txnID;
  }

  @Override
  public TxnType getTransactionType() {
    return TxnType.NORMAL;
  }

  // XXX:: this is server generated txn, hence GID is not assigned.
  @Override
  public GlobalTransactionID getGlobalTransactionID() {
    return GlobalTransactionID.NULL_ID;
  }

  @Override
  public boolean isActiveTxn() {
    return false;
  }

  @Override
  public boolean isResent() {
    return false;
  }

  @Override
  public int getNumApplicationTxn() {
    return 1;
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
    return false;
  }

  @Override
  public boolean isEviction() {
    return false;
  }
}
