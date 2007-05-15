/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ObjectSyncServerTransaction implements ServerTransaction {

  private final TransactionID          txnID;
  private final Set                    oids;
  private final List                   changes;
  private final ObjectStringSerializer serializer;
  private ServerTransactionID          serverTxnID;
  private final Map                    rootsMap;

  public ObjectSyncServerTransaction(TransactionID txnID, Set oids, int dnaCount, ObjectStringSerializer serializer,
                                     List changes, Map rootsMap) {
    this.txnID = txnID;
    this.oids = oids;
    this.serializer = serializer;
    this.changes = changes;
    this.rootsMap = rootsMap;
    this.serverTxnID = new ServerTransactionID(ChannelID.L2_SERVER_ID, txnID);
    Assert.assertEquals(dnaCount, oids.size());
    Assert.assertEquals(dnaCount, changes.size());
  }

  public Collection addNotifiesTo(List list) {
    throw new UnsupportedOperationException();
  }

  public TxnBatchID getBatchID() {
    return TxnBatchID.NULL_BATCH_ID;
  }

  public List getChanges() {
    return changes;
  }

  public ChannelID getChannelID() {
    return ChannelID.L2_SERVER_ID;
  }

  public SequenceID getClientSequenceID() {
    throw new UnsupportedOperationException();
  }

  public DmiDescriptor[] getDmiDescriptors() {
    throw new UnsupportedOperationException();
  }

  public LockID[] getLockIDs() {
    return new LockID[0];
  }

  public Map getNewRoots() {
    return rootsMap;
  }

  public Collection getObjectIDs() {
    return oids;
  }

  /*
   * All objects contained in ObjectSync Message are new Objects for the passive
   */
  public Set getNewObjectIDs() {
    return oids;
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  public ServerTransactionID getServerTransactionID() {
    return serverTxnID;
  }

  public TransactionID getTransactionID() {
    return txnID;
  }

  public TxnType getTransactionType() {
    return TxnType.NORMAL;
  }

  // XXX:: this is server generated txn, hence GID is not assigned.
  public GlobalTransactionID getGlobalTransactionID() {
    return GlobalTransactionID.NULL_ID;
  }

}
