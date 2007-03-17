/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TransactionBatchReaderImpl implements TransactionBatchReader {

  private final TCByteBufferInputStream in;
  private final TxnBatchID              batchID;
  private final ChannelID               source;
  private int                           numTxns;
  private final Collection              acknowledgedTransactionIDs;
  private ObjectStringSerializer        serializer;
  private final boolean passive;

  public TransactionBatchReaderImpl(TCByteBuffer[] data, ChannelID source, Collection acknowledgedTransactionIDs,
                                    ObjectStringSerializer serializer, boolean passive) throws IOException {
    this.passive = passive;
    this.in = new TCByteBufferInputStream(data);
    this.source = source;
    this.batchID = new TxnBatchID(in.readLong());
    this.numTxns = in.readInt();
    Assert.assertNotNull(acknowledgedTransactionIDs);
    this.acknowledgedTransactionIDs = acknowledgedTransactionIDs;
    this.serializer = serializer;
  }

  public ChannelID getChannelID() {
    return this.source;
  }

  public ServerTransaction getNextTransaction() throws IOException {
    if (numTxns == 0) {
      int bytesRemaining = in.available();
      if (bytesRemaining != 0) { throw new IOException(bytesRemaining + " bytes remaining (expecting 0)"); }
      return null;
    }

    // TODO: use factory to avoid dupe instances
    TransactionID txnID = new TransactionID(in.readLong());
    TxnType txnType = TxnType.typeFor(in.readByte());

    SequenceID sequenceID = new SequenceID(in.readLong());

    final int numLocks = in.readInt();
    LockID[] locks = new LockID[numLocks];
    for (int i = 0; i < numLocks; i++) {
      // TODO: use factory to avoid dupe instances
      locks[i] = new LockID(in.readString());
    }

    Map newRoots = new HashMap();
    final int numNewRoots = in.readInt();
    for (int i = 0; i < numNewRoots; i++) {
      String name = in.readString();

      // TODO: use factory to avoid dupe instances
      ObjectID id = new ObjectID(in.readLong());
      newRoots.put(name, id);
    }

    List notifies = new LinkedList();
    final int numNotifies = in.readInt();
    for (int i = 0; i < numNotifies; i++) {
      Notify n = new Notify();
      n.deserializeFrom(in);
      notifies.add(n);
    }

    final int dmiCount = in.readInt();
    final DmiDescriptor[] dmis = new DmiDescriptor[dmiCount];
    for (int i = 0; i < dmiCount; i++) {
      DmiDescriptor dd = new DmiDescriptor();
      dd.deserializeFrom(in);
      dmis[i] = dd;
    }

    List dnas = new ArrayList();
    final int numDNA = in.readInt();
    for (int i = 0; i < numDNA; i++) {
      DNAImpl dna = new DNAImpl(serializer, true);
      dna.deserializeFrom(in);
      dnas.add(dna);
    }

    numTxns--;
    return new ServerTransactionImpl(getBatchID(), txnID, sequenceID, locks, source, dnas, serializer, newRoots,
                                     txnType, notifies, dmis, passive);
  }

  public TxnBatchID getBatchID() {
    return this.batchID;
  }

  public int getNumTxns() {
    return this.numTxns;
  }

  public Collection addAcknowledgedTransactionIDsTo(Collection c) {
    for (Iterator iter = acknowledgedTransactionIDs.iterator(); iter.hasNext();) {
      c.add(new ServerTransactionID(source, (TransactionID) iter.next()));
    }
    return c;
  }
}
