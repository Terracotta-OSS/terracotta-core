/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an atomic change to the states of objects on the server
 */
public class ServerTransactionImpl implements ServerTransaction {
  private final ServerTransactionID    serverTxID;
  private final SequenceID             seqID;
  private final List                   changes;
  private final LockID[]               lockIDs;
  private final TransactionID          txID;
  private final Map                    newRoots;
  private final NodeID                 sourceID;
  private final TxnType                transactionType;
  private final ObjectStringSerializer serializer;
  private final Collection             notifies;
  private final DmiDescriptor[]        dmis;
  private final Set                    objectIDs;
  private final Set                    newObjectIDs;
  private final TxnBatchID             batchID;
  private final GlobalTransactionID    globalTxnID;
  private final int                    numApplicationTxn;

  public ServerTransactionImpl(GlobalTransactionIDGenerator gtxm, TxnBatchID batchID, TransactionID txID,
                               SequenceID sequenceID, LockID[] lockIDs, NodeID source, List dnas,
                               ObjectStringSerializer serializer, Map newRoots, TxnType transactionType,
                               Collection notifies, DmiDescriptor[] dmis, int numApplicationTxn) {
    this.batchID = batchID;
    this.txID = txID;
    this.seqID = sequenceID;
    this.lockIDs = lockIDs;
    this.newRoots = newRoots;
    this.sourceID = source;
    this.numApplicationTxn = numApplicationTxn;
    this.serverTxID = new ServerTransactionID(source, txID);
    // NOTE::XXX:: GlobalTransactionID is assigned in the process transaction stage. The transaction could be
    // re-ordered before apply. This is not a problem because for an transaction to be re-ordered, it should not
    // have any common objects between them. hence if g1 is the first txn and g2 is the second txn, g2 can be applied
    // before g1 only when g2 has no common objects(or locks) with g1. If this is not true then we cant assign gid here.
    this.globalTxnID = gtxm.getOrCreateGlobalTransactionID(serverTxID);
    this.transactionType = transactionType;
    this.notifies = notifies;
    this.dmis = dmis;
    this.changes = dnas;
    this.serializer = serializer;
    Set ids = new HashSet(changes.size());
    HashSet newIDs = new HashSet(changes.size());
    boolean added = true;
    for (Iterator i = changes.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      added &= ids.add(dna.getObjectID());
      if (!dna.isDelta()) {
        newIDs.add(dna.getObjectID());
      }
    }
    Assert.assertTrue(added);
    this.objectIDs = ids;
    this.newObjectIDs = newIDs;
  }

  public int getNumApplicationTxn() {
    return numApplicationTxn;
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  public LockID[] getLockIDs() {
    return lockIDs;
  }

  public NodeID getSourceID() {
    return sourceID;
  }

  public TransactionID getTransactionID() {
    return txID;
  }

  public SequenceID getClientSequenceID() {
    return seqID;
  }

  public List getChanges() {
    return changes;
  }

  public Map getNewRoots() {
    return newRoots;
  }

  public TxnType getTransactionType() {
    return transactionType;
  }

  public Set getObjectIDs() {
    return this.objectIDs;
  }

  public Set getNewObjectIDs() {
    return this.newObjectIDs;
  }

  public Collection getNotifies() {
    return notifies;
  }

  public DmiDescriptor[] getDmiDescriptors() {
    return dmis;
  }

  public String toString() {
    return "ServerTransaction[" + seqID + " , " + txID + "," + sourceID + "," + transactionType + "] = { changes = "
           + changes.size() + ", notifies = " + notifies.size() + ", newRoots = " + newRoots.size() + ", numTxns = "
           + getNumApplicationTxn() + ", oids =  " + objectIDs + ", newObjectIDs = " + newObjectIDs +
           ",\n" + getChangesDetails() +
           " }";
  }

  private String getChangesDetails() {
    StringBuilder sb = new StringBuilder();
    for (Iterator i = changes.iterator(); i.hasNext();) {
      DNA dna = (DNA) i.next();
      sb.append("\t").append(dna.toString()).append("\n");
    }
    return sb.toString();
  }

  public ServerTransactionID getServerTransactionID() {
    return serverTxID;
  }

  public TxnBatchID getBatchID() {
    return batchID;
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return this.globalTxnID;
  }

  public boolean needsBroadcast() {
    return true;
  }
}
