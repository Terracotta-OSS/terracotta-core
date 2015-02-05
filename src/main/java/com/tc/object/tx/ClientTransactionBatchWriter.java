/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.lang.Recyclable;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIDSerializer;
import com.tc.object.locks.Notify;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ClientTransactionBatchWriter implements ClientTransactionBatch {

  private final CommitTransactionMessageFactory commitTransactionMessageFactory;
  private final TxnBatchID                      batchID;
  private final LinkedHashMap<TransactionID, TransactionBuffer> transactionData = new LinkedHashMap<TransactionID, TransactionBuffer>();
  private final ObjectStringSerializer          serializer;
  private final DNAEncodingInternal             encoding;
  private final List<TCByteBufferOutputStream>  batchDataOutputStreams = new ArrayList<TCByteBufferOutputStream>();

  private short                                 outstandingWriteCount  = 0;
  private int                                   numTxns   = 0;
  private boolean                               containsSyncWriteTxn   = false;
  private boolean                               committed              = false;
  private int                                   holders                = 0;

  public ClientTransactionBatchWriter(TxnBatchID batchID,
                                      ObjectStringSerializer serializer, DNAEncodingInternal encoding,
                                      CommitTransactionMessageFactory commitTransactionMessageFactory) {   
    this.batchID = batchID;
    this.encoding = encoding;
    this.commitTransactionMessageFactory = commitTransactionMessageFactory;
    this.serializer = serializer;
  }

  @Override
  public synchronized String toString() {
    return super.toString() + "[" + this.batchID + ", isEmpty=" + isEmpty() + ", numTxns=" + this.numTxns + "]";
  }

  @Override
  public synchronized TxnBatchID getTransactionBatchID() {
    try {
      while (holders > 0) {
        this.wait();
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
    return this.batchID;
  }

  @Override
  public synchronized boolean isEmpty() {
    return this.transactionData.isEmpty();
  }

  @Override
  public synchronized int numberOfTxns() {
    return this.numTxns;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public synchronized TransactionBuffer removeTransaction(TransactionID txID) {
    final TransactionBufferImpl removed = (TransactionBufferImpl) this.transactionData.remove(txID);
    if (removed == null) { throw new AssertionError("Attempt to remove a transaction that doesn't exist"); }
    // if we get some ACKs from the previous instance of the server after we resend this
    // transaction, but before we write to the network, then we don't recycle. We lose those
    // buffers. But since it is a rare scenario we don't lose much, but this check avoid writing
    // corrupt buffers.
    if (this.outstandingWriteCount == 0) {
      removed.recycle();
    }
    return removed;
  }

  @Override
  public synchronized boolean contains(TransactionID txID) {
    return this.transactionData.containsKey(txID);
  }

  private TransactionBuffer createBuffer(ClientTransaction txn) {

    final TransactionBuffer txnBuffer = createTransactionBuffer(txn.getSequenceID(), newOutputStream(),
                                                                this.serializer, this.encoding, txn.getTransactionID());

    this.transactionData.put(txn.getTransactionID(), txnBuffer);

    return txnBuffer;
  }

  // Overridden in active-active
  protected TransactionBuffer createTransactionBuffer(SequenceID sid,
                                                      TCByteBufferOutputStream newOutputStream,
                                                      ObjectStringSerializer objectStringserializer,
                                                      DNAEncodingInternal dnaEncoding, TransactionID txnID) {
    return new TransactionBufferImpl(this, sid, newOutputStream, objectStringserializer, dnaEncoding, txnID);
  }

  @Override
  public synchronized TransactionBuffer addTransaction(ClientTransaction txn) {
    holders += 1;

    if (committed) { throw new AssertionError("Already committed"); }

    this.numTxns++;

    if (txn.getLockType().equals(TxnType.SYNC_WRITE)) {
      this.containsSyncWriteTxn = true;
    }

    final TransactionBuffer txnBuffer = createBuffer(txn);

    txnBuffer.addTransactionCompleteListeners(txn.getTransactionCompleteListeners());

    return txnBuffer;
  }

  private synchronized void release() {
    if (--holders == 0) {
      notify();
    }
  }

  @SuppressWarnings("resource")
  @Override
  public synchronized TCByteBuffer[] getData() {
    this.committed = true;

    this.outstandingWriteCount++;
    final TCByteBufferOutputStream out = newOutputStream();
    writeHeader(out);
    for (TransactionBuffer tb : this.transactionData.values()) {
      tb.writeTo(out);
    }
    this.batchDataOutputStreams.add(out);

    return out.toArray();
  }

  protected void writeHeader(TCByteBufferOutputStream out) {
    out.writeLong(this.batchID.toLong());
    out.writeInt(this.transactionData.size());
    out.writeBoolean(this.containsSyncWriteTxn);
  }

  private TCByteBufferOutputStream newOutputStream() {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream(32, 4096, false);
    return out;
  }

  /*
   * message send is outside the sync, as it can deadlock with OOO StateMachines. Especially, ReceiveStateMachine remove
   * and reallyRecycle of messages can get stuck waiting for monitor entry at TransactionBatchWriter while Transaction
   * Batch send stage is trying to send messages.
   */
  @Override
  public void send() {
    final CommitTransactionMessage msg;
    synchronized (this) {
      msg = this.commitTransactionMessageFactory.newCommitTransactionMessage();
      msg.setBatch(this, this.serializer);
    }
    msg.send();
  }

  @Override
  public synchronized Collection<TransactionID> addTransactionIDsTo(Collection<TransactionID> c) {
    c.addAll(this.transactionData.keySet());
    return c;
  }

  @Override
  public synchronized SequenceID getMinTransactionSequence() {
    return this.transactionData.isEmpty() ? SequenceID.NULL_ID : ((TransactionBufferImpl) this.transactionData.values()
        .iterator().next()).getSequenceID();
  }

  @Override
  public Collection<SequenceID> addTransactionSequenceIDsTo(Collection<SequenceID> sequenceIDs) {
    for (TransactionBuffer buf : this.transactionData.values()) {
      final TransactionBufferImpl tb = ((TransactionBufferImpl) buf);
      sequenceIDs.add(tb.getSequenceID());
    }
    return sequenceIDs;
  }

  @Override
  public synchronized void recycle() {
    for (TCByteBufferOutputStream buffer : this.batchDataOutputStreams) {
      buffer.recycle();
    }
    this.batchDataOutputStreams.clear();
    this.outstandingWriteCount--;
  }

  @Override
  public synchronized String dump() {
    final StringBuffer sb = new StringBuffer("TransactionBatchWriter = { \n");
    for (Map.Entry<TransactionID, TransactionBuffer> entry : this.transactionData.entrySet()) {
      sb.append(entry.getKey()).append(" = ");
      sb.append(((TransactionBufferImpl) entry.getValue()).dump());
      sb.append("\n");
    }
    return sb.append(" } ").toString();
  }

  protected static class TransactionBufferImpl implements Recyclable, TransactionBuffer {

    private static final int                      UNINITIALIZED_LENGTH = -1;

    private final ClientTransactionBatchWriter    parent;
    private final SequenceID                      sequenceID;
    private final TCByteBufferOutputStream        output;
    private final ObjectStringSerializer          serializer;
    private final DNAEncodingInternal             encoding;
    private final SetOnceFlag                     committed            = new SetOnceFlag();

    private final Map<ObjectID, DNAWriter>        writers              = new LinkedHashMap<ObjectID, DNAWriter>();
    private final TransactionID                   txnID;

    private int                                   headerLength         = UNINITIALIZED_LENGTH;
    private int                                   txnCount             = 0;
    private Mark                                  changesCountMark;
    private Mark                                  txnCountMark;
    private List<TransactionCompleteListener>     txnCompleteListers;

    TransactionBufferImpl(ClientTransactionBatchWriter parent, SequenceID sequenceID,
                          TCByteBufferOutputStream output, ObjectStringSerializer serializer,
                          DNAEncodingInternal encoding, TransactionID txnID) {
      this.parent = parent;
      this.sequenceID = sequenceID;
      this.output = output;
      this.serializer = serializer;
      this.encoding = encoding;
      this.txnID = txnID;
    }

    @Override
    public TransactionID getTransactionID() {
      return this.txnID;
    }

    @Override
    public void writeTo(TCByteBufferOutputStream dest) {
      // XXX: make a writeInt() and writeLong() methods on Mark. Maybe ones that take offsets too

      // This check is needed since this buffer might need to be resent upon server crash
      if (this.committed.attemptSet()) {
        this.txnCountMark.write(Conversion.int2Bytes(this.txnCount));
        this.changesCountMark.write(Conversion.int2Bytes(this.writers.size()));

        for (DNAWriter writer : this.writers.values()) {
          writer.finalizeHeader();
        }
      }

      dest.write(this.output.toArray());
      return;
    }

    String dump() {
      return " { " + this.sequenceID + " , Objects in Txn : " + this.writers.size() + " }";
    }

    SequenceID getSequenceID() {
      return this.sequenceID;
    }

    @Override
    public int write(ClientTransaction txn) {
      try {
        final int start = this.output.getBytesWritten();

        if (this.txnCount == 0) {
          writeFirst(txn);
        } else {
          appendChanges(txn);
        }

        this.txnCount++;

        return this.output.getBytesWritten() - start;
      } finally {
        parent.release();
      }
    }

    private void appendChanges(ClientTransaction txn) {
      writeChange(txn.getChangeBuffer());
    }

    private void writeChange(TCChangeBuffer buffer) { 
      if (buffer == null) {
        // XXX: This isn't great. An txn with no actions or new objects does this at the moment
        return;
      }
      
      final TCObject tco = buffer.getTCObject();
      final ObjectID oid = tco.getObjectID();
      final boolean isNew = tco.isNew();

      DNAWriter writer = this.writers.get(oid);
      if (writer == null) {
        writer = new DNAWriterImpl(this.output, oid, tco.getClassName(), this.serializer, this.encoding, !isNew);

        this.writers.put(oid, writer);
      } else {
        throw new AssertionError("writer already exists for " + oid);
      }

      // this isNew() check and flipping of the new flag are safe here only because transaction writing is completely
      // serialized within the the batch writer. This ensure there is no race for more than one thread to commit the
      // same "new" object
      if (isNew) {
        tco.dehydrate(writer);
        tco.setNotNew();
      }

      buffer.writeTo(writer);

      writer.markSectionEnd();
    }

    private void writeFirst(ClientTransaction txn) {
      writeTransactionHeader(txn);
      writeChange(txn.getChangeBuffer());
    }

    private void writeTransactionHeader(ClientTransaction txn) {
      final int startPos = this.output.getBytesWritten();

      // /////////////////////////////////////////////////////////////////////////////////////////
      // If you're modifying this format, you'll need to update
      // TransactionBatchReader as well
      // /////////////////////////////////////////////////////////////////////////////////////////

      final TransactionID tid = txn.getTransactionID();
      if (tid.isNull()) { throw new AssertionError("Writing Transaction with null Transaction ID : " + txn.toString()); }

      this.output.writeLong(tid.toLong());
      this.output.writeByte(txn.getLockType().getType());
      this.txnCountMark = this.output.mark();
      this.output.writeInt(UNINITIALIZED_LENGTH);
      final SequenceID sid = txn.getSequenceID();
      if (sid.isNull()) { throw new AssertionError("SequenceID is null: " + txn); }
      this.output.writeLong(sid.toLong());

      // isEviction
      this.output.writeBoolean(false);

      final Collection<LockID> locks = txn.getAllLockIDs();
      this.output.writeInt(locks.size());
      for (LockID lock : locks) {
        new LockIDSerializer(lock).serializeTo(this.output);
      }

      final Map<String, ObjectID> newRoots = txn.getNewRoots();
      this.output.writeInt(newRoots.size());
      for (Entry<String, ObjectID> entry : newRoots.entrySet()) {
        final String name = entry.getKey();
        final ObjectID id = entry.getValue();
        this.output.writeString(name);
        this.output.writeLong(id.toLong());
      }

      final List<Notify> notifies = txn.getNotifies();
      this.output.writeInt(notifies.size());
      for (Notify n : notifies) {
        n.serializeTo(this.output);
      }

      writeAdditionalHeaderInformation(this.output, txn);

      this.changesCountMark = this.output.mark();
      this.output.writeInt(-1);

      Assert.assertEquals(UNINITIALIZED_LENGTH, this.headerLength);
      this.headerLength = this.output.getBytesWritten() - startPos;

    }

    protected void writeAdditionalHeaderInformation(TCByteBufferOutputStream out, ClientTransaction txn) {
      // This is here so that the format is compatible with active-active
      writeLongArray(out, new long[0]);
    }

    protected void writeLongArray(TCByteBufferOutputStream out, long[] ls) {
      out.writeInt(ls.length);
      for (final long element : ls) {
        out.writeLong(element);
      }
    }

    @Override
    public int getTxnCount() {
      return this.txnCount;
    }

    @Override
    public void recycle() {
      this.output.recycle();
    }

    @Override
    public void addTransactionCompleteListeners(List<TransactionCompleteListener> transactionCompleteListeners) {
      if (!transactionCompleteListeners.isEmpty()) {
        if (txnCompleteListers == null) {
          txnCompleteListers = new ArrayList<TransactionCompleteListener>(5);
        }
        txnCompleteListers.addAll(transactionCompleteListeners);
      }
    }

    @Override
    public List<TransactionCompleteListener> getTransactionCompleteListeners() {
      return txnCompleteListers == null ? Collections.emptyList() : txnCompleteListers;
    }
  }

}
