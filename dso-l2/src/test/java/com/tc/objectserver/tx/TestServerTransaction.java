/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TestServerTransaction implements ServerTransaction {

  public List                       changes = new ArrayList();
  public ObjectIDSet                oids    = new ObjectIDSet();
  private final ServerTransactionID sid;
  private final TxnBatchID          bid;
  private final GlobalTransactionID gtid;
  public long[]                     hwm;

  public TestServerTransaction(ServerTransactionID sid, TxnBatchID bid) {
    this(sid, bid, null);
  }

  public TestServerTransaction(ServerTransactionID sid, TxnBatchID bid, GlobalTransactionID gtid) {
    this.sid = sid;
    this.bid = bid;
    this.gtid = gtid;
  }

  public ObjectStringSerializer getSerializer() {
    throw new ImplementMe();
  }

  public LockID[] getLockIDs() {
    return new LockID[] { new StringLockID("saro") };
  }

  public NodeID getSourceID() {
    return this.sid.getSourceID();
  }

  public TransactionID getTransactionID() {
    return this.sid.getClientTransactionID();
  }

  public SequenceID getClientSequenceID() {
    return SequenceID.NULL_ID;
  }

  public List getChanges() {
    return this.changes;
  }

  public Map getNewRoots() {
    return Collections.EMPTY_MAP;
  }

  public TxnType getTransactionType() {
    throw new ImplementMe();
  }

  public ObjectIDSet getObjectIDs() {
    return this.oids;
  }

  public Collection getNotifies() {
    return Collections.EMPTY_LIST;
  }

  public ServerTransactionID getServerTransactionID() {
    return this.sid;
  }

  public TxnBatchID getBatchID() {
    return this.bid;
  }

  public DmiDescriptor[] getDmiDescriptors() {
    throw new ImplementMe();
  }
  
  public MetaDataReader[] getMetaDataReaders() {
    return new MetaDataReader[0];
  }

  public boolean isPassive() {
    return false;
  }

  public ObjectIDSet getNewObjectIDs() {
    throw new ImplementMe();
  }

  public GlobalTransactionID getGlobalTransactionID() {
    if (this.gtid != null) { return this.gtid; }
    throw new AssertionError("Gid is not set !");
  }

  public boolean isActiveTxn() {
    return true;
  }

  public int getNumApplicationTxn() {
    return 1;
  }

  public void setGlobalTransactionID(GlobalTransactionID gid) {
    throw new ImplementMe();
  }

  public long[] getHighWaterMarks() {
    return this.hwm;
  }

}