/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput.Mark;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIDSerializer;
import com.tc.object.locks.Notify;
import com.tc.object.locks.NotifyImpl;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.util.SequenceID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Note: If the format of the Transaction Batch changes, then it has to be reflected in three place.<br>
 * 1) This file - {@link com.tc.objectserver.tx.TransactionBatchReaderImpl} <br>
 * 2) The client side writer - {@link com.tc.object.tx.TrasactionBatchWriter} and <br>
 * 3) The server side writer - {@link com.tc.objectserver.tx.ServerTransactionBatchWriter}
 */
public class TransactionBatchReaderImpl implements TransactionBatchReader {

  private static final TCLogger                        logger      = TCLogging
                                                                       .getLogger(TransactionBatchReaderImpl.class);

  private static final int                             HEADER_SIZE = 13;

  private final TCByteBufferInputStream                in;
  private final TxnBatchID                             batchID;
  private final NodeID                                 source;
  private final int                                    numTxns;
  private int                                          txnToRead;
  private final ObjectStringSerializer                 serializer;
  private final ServerTransactionFactory               txnFactory;
  private final LinkedHashMap<TransactionID, MarkInfo> marks       = new LinkedHashMap<TransactionID, MarkInfo>();
  private final TCByteBuffer[]                         data;
  private final boolean                                containsSyncWriteTransaction;

  public TransactionBatchReaderImpl(final TCByteBuffer[] data, final NodeID nodeID,
                                    final ObjectStringSerializer serializer, final ServerTransactionFactory txnFactory,
                                    final DSOGlobalServerStats globalSeverStats) throws IOException {
    this.data = data;
    this.txnFactory = txnFactory;
    this.in = new TCByteBufferInputStream(data);
    this.source = nodeID;
    this.batchID = new TxnBatchID(this.in.readLong());
    this.numTxns = this.in.readInt();
    this.txnToRead = this.numTxns;
    this.serializer = serializer;
    this.containsSyncWriteTransaction = this.in.readBoolean();
    if (globalSeverStats != null) {
      // transactionSize = Sum of Size of transactions / number of transactions
      globalSeverStats.getTransactionSizeCounter().increment(this.in.getTotalLength(), this.numTxns);
    }
  }

  public boolean containsSyncWriteTransaction() {
    return this.containsSyncWriteTransaction;
  }

  private TCByteBuffer[] getHeaderBuffers(final int txnsCount) {
    final TCByteBufferOutputStream tos = new TCByteBufferOutputStream(HEADER_SIZE, false);
    tos.writeLong(this.batchID.toLong());
    tos.writeInt(txnsCount);
    tos.writeBoolean(this.containsSyncWriteTransaction);
    return tos.toArray();
  }

  private long[] readLongArray(final TCByteBufferInputStream input) throws IOException {
    final int size = input.readInt();
    final long larray[] = new long[size];
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
      final int bytesRemaining = this.in.available();
      if (bytesRemaining != 0) { throw new IOException(bytesRemaining + " bytes remaining (expecting 0)"); }
      return null;
    }
    final Mark start = this.in.mark();
    final TransactionID txnID = new TransactionID(this.in.readLong());
    final TxnType txnType = TxnType.typeFor(this.in.readByte());

    final int numApplictionTxn = this.in.readInt();

    final SequenceID sequenceID = new SequenceID(this.in.readLong());

    final int numLocks = this.in.readInt();
    final LockID[] locks = new LockID[numLocks];
    for (int i = 0; i < numLocks; i++) {
      final LockIDSerializer lidsr = new LockIDSerializer();
      locks[i] = ((LockIDSerializer) lidsr.deserializeFrom(this.in)).getLockID();
    }

    final Map newRoots = new HashMap();
    final int numNewRoots = this.in.readInt();
    for (int i = 0; i < numNewRoots; i++) {
      final String name = this.in.readString();

      final ObjectID id = new ObjectID(this.in.readLong());
      newRoots.put(name, id);
    }

    final List notifies = new LinkedList();
    final int numNotifies = this.in.readInt();
    for (int i = 0; i < numNotifies; i++) {
      final Notify n = new NotifyImpl();
      n.deserializeFrom(this.in);
      notifies.add(n);
    }

    final int dmiCount = this.in.readInt();
    final DmiDescriptor[] dmis = new DmiDescriptor[dmiCount];
    for (int i = 0; i < dmiCount; i++) {
      final DmiDescriptor dd = new DmiDescriptor();
      dd.deserializeFrom(this.in);
      dmis[i] = dd;
    }

    final long[] highwaterMarks = readLongArray(this.in);

    final List dnas = new ArrayList();
    final int numDNA = this.in.readInt();
    final List<MetaDataReader> metaDataReaders = new ArrayList<MetaDataReader>();
    
    for (int i = 0; i < numDNA; i++) {
      final DNAImpl dna = new DNAImpl(this.serializer, true);
      dna.deserializeFrom(this.in);

     if( dna.getMetaDataReader() != DNAImpl.NULL_META_DATA_READER) {
       metaDataReaders.add(dna.getMetaDataReader());
     }
      
      if (dna.isDelta() && dna.getActionCount() < 1) {
        // This is really unexpected and indicates an error in the client, but the server
        // should not be harmed by it (other than extra processing)
        logger.warn("received delta dna with no actions: " + dna);
      }

      // to be on the safe side, still adding all DNAs received to the list even if they
      // triggered the error logging above
      dnas.add(dna);
    }
    final Mark end = this.in.mark();
    this.marks.put(txnID, new MarkInfo(this.numTxns - this.txnToRead, start, end));

    this.txnToRead--;
    MetaDataReader [] metaDataReadersArr = metaDataReaders.toArray(new MetaDataReader[metaDataReaders.size()]);
    return this.txnFactory.createServerTransaction(getBatchID(), txnID, sequenceID, locks, this.source, dnas,
                                                   this.serializer, newRoots, txnType, notifies, dmis,
                                                   metaDataReadersArr, numApplictionTxn, highwaterMarks);
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

  public TCByteBuffer[] getBackingBuffers(final ServerTransactionID from, final ServerTransactionID to) {
    if (!from.getSourceID().equals(this.source) || !to.getSourceID().equals(this.source)) {
      // Not the same source
      throw new AssertionError("Source is not the same : " + this.source + " : " + from + " , " + to);
    }

    final MarkInfo fromMark = this.marks.get(from.getClientTransactionID());
    final MarkInfo toMark = this.marks.get(to.getClientTransactionID());

    if (fromMark.getIndex() > toMark.getIndex()) { throw new AssertionError("From Tid " + from + " is after To Tid : "
                                                                            + to); }
    final int noOfTxn = toMark.getIndex() - fromMark.getIndex() + 1;

    if (noOfTxn == this.numTxns) {
      // All transactions are requested
      return getBackingBuffers();
    }
    final TCByteBuffer[] header = getHeaderBuffers(noOfTxn);
    final TCByteBuffer[] content = this.in.toArray(fromMark.getStart(), toMark.getEnd());

    final TCByteBuffer[] fullContents = new TCByteBuffer[header.length + content.length];
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

    public MarkInfo(final int index, final Mark start, final Mark end) {
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
