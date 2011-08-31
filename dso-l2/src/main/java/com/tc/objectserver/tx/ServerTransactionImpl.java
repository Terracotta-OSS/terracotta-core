/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDAlreadySetException;
import com.tc.object.locks.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
  private final MetaDataReader[]       metaDataReaders;
  private final ObjectIDSet            objectIDs;
  private final ObjectIDSet            newObjectIDs;
  private final TxnBatchID             batchID;
  private final int                    numApplicationTxn;
  private final long[]                 highWaterMarks;

  private GlobalTransactionID          globalTxnID;

  public ServerTransactionImpl(TxnBatchID batchID, TransactionID txID, SequenceID sequenceID, LockID[] lockIDs,
                               NodeID source, List dnas, ObjectStringSerializer serializer, Map newRoots,
                               TxnType transactionType, Collection notifies, DmiDescriptor[] dmis,
                               MetaDataReader[] metaDataReaders, int numApplicationTxn, long[] highWaterMarks) {
    this.batchID = batchID;
    this.txID = txID;
    this.seqID = sequenceID;
    this.lockIDs = lockIDs;
    this.newRoots = newRoots;
    this.sourceID = source;
    this.numApplicationTxn = numApplicationTxn;
    this.highWaterMarks = highWaterMarks;
    this.serverTxID = new ServerTransactionID(source, txID);
    this.transactionType = transactionType;
    this.notifies = notifies;
    this.dmis = dmis;
    this.metaDataReaders = metaDataReaders;
    this.changes = dnas;
    this.serializer = serializer;
    final ObjectIDSet ids = new ObjectIDSet();
    final ObjectIDSet newIDs = new ObjectIDSet();
    boolean added = true;
    for (final Iterator i = this.changes.iterator(); i.hasNext();) {
      final DNA dna = (DNA) i.next();
      added &= ids.add(dna.getObjectID());
      if (!dna.isDelta()) {
        newIDs.add(dna.getObjectID());
      }
    }
    Assert.assertTrue(added);
    this.objectIDs = ids;
    this.newObjectIDs = newIDs;
  }

  // NOTE::XXX:: GlobalTransactionID is assigned in the process transaction stage. The transaction could be
  // re-ordered before apply. This is not a problem because for an transaction to be re-ordered, it should not
  // have any common objects between them. hence if g1 is the first txn and g2 is the second txn, g2 can be applied
  // before g1 only when g2 has no common objects(or locks) with g1. If this is not true then we cant assign gid here.
  public void setGlobalTransactionID(final GlobalTransactionID gid) throws GlobalTransactionIDAlreadySetException {
    if (this.globalTxnID != null) { throw new GlobalTransactionIDAlreadySetException("Gid already assigned : " + this
                                                                                     + " gid : " + gid); }
    this.globalTxnID = gid;
  }

  public int getNumApplicationTxn() {
    return this.numApplicationTxn;
  }

  public ObjectStringSerializer getSerializer() {
    return this.serializer;
  }

  public LockID[] getLockIDs() {
    return this.lockIDs;
  }

  public NodeID getSourceID() {
    return this.sourceID;
  }

  public TransactionID getTransactionID() {
    return this.txID;
  }

  public SequenceID getClientSequenceID() {
    return this.seqID;
  }

  public List getChanges() {
    return this.changes;
  }

  public Map getNewRoots() {
    return this.newRoots;
  }

  public TxnType getTransactionType() {
    return this.transactionType;
  }

  public ObjectIDSet getObjectIDs() {
    return this.objectIDs;
  }

  public ObjectIDSet getNewObjectIDs() {
    return this.newObjectIDs;
  }

  public Collection getNotifies() {
    return this.notifies;
  }

  public DmiDescriptor[] getDmiDescriptors() {
    return this.dmis;
  }

  public MetaDataReader[] getMetaDataReaders() {
    return this.metaDataReaders;
  }

  @Override
  public String toString() {
    return "ServerTransaction[" + this.seqID + " , " + this.txID + "," + this.sourceID + "," + this.transactionType
           + ", HighWaterMarks: " + Arrays.toString(this.highWaterMarks) + "] = { changes = " + this.changes.size()
           + ", notifies = " + this.notifies.size() + ", newRoots = " + this.newRoots.size() + ", numTxns = "
           + getNumApplicationTxn() + ", oids =  " + this.objectIDs + ", newObjectIDs = " + this.newObjectIDs + ",\n"
           + getChangesDetails() + " }";
  }

  private String getChangesDetails() {
    final StringBuilder sb = new StringBuilder();
    for (final Iterator i = this.changes.iterator(); i.hasNext();) {
      final DNA dna = (DNA) i.next();
      sb.append("\t").append(dna.toString()).append("\n");
    }
    return sb.toString();
  }

  public ServerTransactionID getServerTransactionID() {
    return this.serverTxID;
  }

  public TxnBatchID getBatchID() {
    return this.batchID;
  }

  public GlobalTransactionID getGlobalTransactionID() {
    if (this.globalTxnID == null) { throw new AssertionError("Gid not assigned : " + this); }
    return this.globalTxnID;
  }

  public boolean isActiveTxn() {
    return true;
  }

  public long[] getHighWaterMarks() {
    return this.highWaterMarks;
  }
}
