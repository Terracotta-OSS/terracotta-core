/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrunedServerTransaction implements ServerTransaction {

  private final List              prunedChanges;
  private final ServerTransaction orgTxn;
  private final Collection        oids;

  public PrunedServerTransaction(List prunedChanges, ServerTransaction st) {
    this.prunedChanges = prunedChanges;
    this.orgTxn = st;
    List ids = new ArrayList(prunedChanges.size());
    for (Iterator i = prunedChanges.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      ids.add(dna.getObjectID());
    }
    this.oids = ids;
  }

  public Collection addNotifiesTo(List list) {
    throw new AssertionError("Cant add notifies to pruned transaction");
  }

  public TxnBatchID getBatchID() {
    return orgTxn.getBatchID();
  }

  public List getChanges() {
    return prunedChanges;
  }

  public ChannelID getChannelID() {
    return orgTxn.getChannelID();
  }

  public DmiDescriptor[] getDmiDescriptors() {
    return orgTxn.getDmiDescriptors();
  }

  public LockID[] getLockIDs() {
    return orgTxn.getLockIDs();
  }

  public Set getNewObjectIDs() {
    return orgTxn.getNewObjectIDs();
  }

  public Map getNewRoots() {
    return orgTxn.getNewRoots();
  }

  public Collection getObjectIDs() {
    return oids;
  }

  public ObjectStringSerializer getSerializer() {
    return orgTxn.getSerializer();
  }

  public ServerTransactionID getServerTransactionID() {
    return orgTxn.getServerTransactionID();
  }

  public TransactionID getTransactionID() {
    return orgTxn.getTransactionID();
  }

  public TxnType getTransactionType() {
    return orgTxn.getTransactionType();
  }

  public SequenceID getClientSequenceID() {
    return orgTxn.getClientSequenceID();
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return orgTxn.getGlobalTransactionID();
  }

}
