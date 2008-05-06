/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.NodeID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestServerTransaction implements ServerTransaction {

  public List                       changes = new ArrayList();
  public Set                        oids    = new HashSet();
  private ServerTransactionID       sid;
  private TxnBatchID                bid;
  private final GlobalTransactionID gtid;

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
    return new LockID[] { new LockID("saro") };
  }

  public NodeID getSourceID() {
    return sid.getSourceID();
  }

  public TransactionID getTransactionID() {
    return sid.getClientTransactionID();
  }

  public SequenceID getClientSequenceID() {
    return SequenceID.NULL_ID;
  }

  public List getChanges() {
    return changes;
  }

  public Map getNewRoots() {
    return Collections.EMPTY_MAP;
  }

  public TxnType getTransactionType() {
    throw new ImplementMe();
  }

  public Set getObjectIDs() {
    return oids;
  }

  public Collection getNotifies() {
    return Collections.EMPTY_LIST;
  }

  public ServerTransactionID getServerTransactionID() {
    return sid;
  }

  public TxnBatchID getBatchID() {
    return bid;
  }

  public DmiDescriptor[] getDmiDescriptors() {
    throw new ImplementMe();
  }

  public boolean isPassive() {
    return false;
  }

  public Set getNewObjectIDs() {
    throw new ImplementMe();
  }

  public GlobalTransactionID getGlobalTransactionID() {
    if (gtid != null) { return gtid; }
    throw new ImplementMe();
  }

  public boolean needsBroadcast() {
    return true;
  }

  public int getNumApplicationTxn() {
    return 1;
  }

}