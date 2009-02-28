/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferInput.Mark;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
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
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TransactionBatchReaderImpl implements TransactionBatchReader {

  private static final TCLogger                        logger = TCLogging.getLogger(TransactionBatchReaderImpl.class);

  private final TCByteBufferInputStream                in;
  private final TxnBatchID                             batchID;
  private final NodeID                                 source;
  private final int                                    numTxns;
  private int                                          txnToRead;
  private final ObjectStringSerializer                 serializer;
  private final ServerTransactionFactory               txnFactory;
  private final LinkedHashMap<TransactionID, MarkInfo> marks  = new LinkedHashMap<TransactionID, MarkInfo>();
  private final TCByteBuffer[]                         data;

  public TransactionBatchReaderImpl(TCByteBuffer[] data, NodeID nodeID, ObjectStringSerializer serializer,
                                    ServerTransactionFactory txnFactory, DSOGlobalServerStats globalSeverStats)
      throws IOException {
    this.data = data;
    this.txnFactory = txnFactory;
    this.in = new TCByteBufferInputStream(data);
    this.source = nodeID;
    this.batchID = new TxnBatchID(this.in.readLong());
    this.numTxns = this.in.readInt();
    this.txnToRead = this.numTxns;
    this.serializer = serializer;
    Assert.assertNotNull(globalSeverStats);
    Assert.assertNotNull(globalSeverStats.getTransactionSizeCounter());
    // transactionSize = Sum of Size of transactions / number of transactions
    globalSeverStats.getTransactionSizeCounter().increment(this.in.getTotalLength(), this.numTxns);
  }

  private TCByteBuffer[] getHeaderBuffers(int txnsCount) {
    TCByteBufferOutputStream tos = new TCByteBufferOutputStream(12, false);
    tos.writeLong(this.batchID.toLong());
    tos.writeInt(txnsCount);
    return tos.toArray();
  }

  private long[] readLongArray(TCByteBufferInputStream input) throws IOException {
    int size = input.readInt();
    long larray[] = new long[size];
    for (int i = 0; i < larray.length; i++) {
      larray[i] = input.readLong();
    }
    return larray;
  }

  public NodeID getNodeID() {
    return this.source;
  }

  public ServerTransaction getNextTransaction() throws IOException {
    if (this.txnToRead == 0) {
      int bytesRemaining = this.in.available();
      if (bytesRemaining != 0) { throw new IOException(bytesRemaining + " bytes remaining (expecting 0)"); }
      return null;
    }
    Mark start = this.in.mark();
    TransactionID txnID = new TransactionID(this.in.readLong());
    TxnType txnType = TxnType.typeFor(this.in.readByte());

    final int numApplictionTxn = this.in.readInt();

    SequenceID sequenceID = new SequenceID(this.in.readLong());

    final int numLocks = this.in.readInt();
    LockID[] locks = new LockID[numLocks];
    for (int i = 0; i < numLocks; i++) {
      locks[i] = new LockID(this.in.readString());
    }

    Map newRoots = new HashMap();
    final int numNewRoots = this.in.readInt();
    for (int i = 0; i < numNewRoots; i++) {
      String name = this.in.readString();

      ObjectID id = new ObjectID(this.in.readLong());
      newRoots.put(name, id);
    }

    List notifies = new LinkedList();
    final int numNotifies = this.in.readInt();
    for (int i = 0; i < numNotifies; i++) {
      Notify n = new Notify();
      n.deserializeFrom(this.in);
      notifies.add(n);
    }

    final int dmiCount = this.in.readInt();
    final DmiDescriptor[] dmis = new DmiDescriptor[dmiCount];
    for (int i = 0; i < dmiCount; i++) {
      DmiDescriptor dd = new DmiDescriptor();
      dd.deserializeFrom(this.in);
      dmis[i] = dd;
    }

    long[] highwaterMarks = readLongArray(this.in);

    List dnas = new ArrayList();
    final int numDNA = this.in.readInt();
    for (int i = 0; i < numDNA; i++) {
      DNAImpl dna = new DNAImpl(this.serializer, true);
      dna.deserializeFrom(this.in);

      if (dna.isDelta() && dna.getActionCount() < 1) {
        // This is really unexpected and indicates an error in the client, but the server
        // should not be harmed by it (other than extra processing)
        logger.warn("received delta dna with no actions: " + dna);
      }

      // to be on the safe side, still adding all DNAs received to the list even if they
      // triggered the error logging above
      dnas.add(dna);
    }
    Mark end = this.in.mark();
    this.marks.put(txnID, new MarkInfo(this.numTxns - this.txnToRead, start, end));

    this.txnToRead--;
    return this.txnFactory.createServerTransaction(getBatchID(), txnID, sequenceID, locks, this.source, dnas,
                                                   this.serializer, newRoots, txnType, notifies, dmis,
                                                   numApplictionTxn, highwaterMarks);
  }

  public TxnBatchID getBatchID() {
    return this.batchID;
  }

  public int getNumberForTxns() {
    return this.numTxns;
  }

  public TCByteBuffer[] getBackingBuffers() {
    return this.data;
  }

  public TCByteBuffer[] getBackingBuffers(ServerTransactionID from, ServerTransactionID to) {
    if (!from.getSourceID().equals(this.source) || !to.getSourceID().equals(this.source)) {
      // Not the same source
      throw new AssertionError("Source is not the same : " + this.source + " : " + from + " , " + to);
    }

    MarkInfo fromMark = this.marks.get(from.getClientTransactionID());
    MarkInfo toMark = this.marks.get(to.getClientTransactionID());

    if (fromMark.getIndex() > toMark.getIndex()) { throw new AssertionError("From Tid " + from + " is after To Tid : "
                                                                            + to); }
    int noOfTxn = toMark.getIndex() - fromMark.getIndex() + 1;

    if (noOfTxn == this.numTxns) {
      // All transactions are requested
      return getBackingBuffers();
    }
    TCByteBuffer[] header = getHeaderBuffers(noOfTxn);
    TCByteBuffer[] content = this.in.toArray(fromMark.getStart(), toMark.getEnd());

    TCByteBuffer[] fullContents = new TCByteBuffer[header.length + content.length];
    System.arraycopy(header, 0, fullContents, 0, header.length);
    System.arraycopy(content, 0, fullContents, header.length, content.length);

    return fullContents;
  }

  public ObjectStringSerializer getSerializer() {
    return this.serializer;
  }

  private static final class MarkInfo {

    private final Mark start;
    private final int  index;
    private final Mark end;

    public MarkInfo(int index, Mark start, Mark end) {
      this.index = index;
      this.start = start;
      this.end = end;
    }

    public Mark getStart() {
      return this.start;
    }

    public Mark getEnd() {
      return this.end;
    }

    public int getIndex() {
      return this.index;
    }

  }
}
