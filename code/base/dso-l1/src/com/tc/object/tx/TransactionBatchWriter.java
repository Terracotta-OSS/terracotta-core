/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.lang.Recyclable;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.properties.TCProperties;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class TransactionBatchWriter implements ClientTransactionBatch {
  public static final String                    FOLDING_ENABLED_PROP      = "l1.transactionmanager.folding.enabled";
  public static final String                    FOLDING_OBJECT_LIMIT_PROP = "l1.transactionmanager.folding.object.limit";
  public static final String                    FOLDING_LOCK_LIMIT_PROP   = "l1.transactionmanager.folding.lock.limit";

  private static final FoldingKey               DISABLED_FOLDING_KEY      = new DisabledFoldingKey();

  private final CommitTransactionMessageFactory commitTransactionMessageFactory;
  private final TxnBatchID                      batchID;
  private final LinkedHashMap                   transactionData           = new LinkedHashMap();
  private final ObjectStringSerializer          serializer;
  private final DNAEncoding                     encoding;
  private final List                            batchDataOutputStreams    = new ArrayList();

  private final boolean                         foldingEnabled;
  private final int                             foldingObjectLimit;
  private final int                             foldingLockLimit;

  private short                                 outstandingWriteCount     = 0;
  private int                                   bytesWritten              = 0;

  private int                                   numFolded                 = 0;
  private int                                   numTxns                   = 0;

  public TransactionBatchWriter(TxnBatchID batchID, ObjectStringSerializer serializer, DNAEncoding encoding,
                                CommitTransactionMessageFactory commitTransactionMessageFactory, TCProperties tcProps) {
    this.batchID = batchID;
    this.encoding = encoding;
    this.commitTransactionMessageFactory = commitTransactionMessageFactory;
    this.serializer = serializer;

    this.foldingEnabled = tcProps.getBoolean(FOLDING_ENABLED_PROP, true);
    this.foldingLockLimit = tcProps.getInt(FOLDING_LOCK_LIMIT_PROP, 0);
    this.foldingObjectLimit = tcProps.getInt(FOLDING_OBJECT_LIMIT_PROP, 0);
  }

  public synchronized String toString() {
    return super.toString() + "[" + this.batchID + ", isEmpty=" + isEmpty() + ", numTxnsBeforeFolding=" + numTxns
           + ", numFolds=" + numFolded;
  }

  public synchronized int numberOfFolds() {
    return numFolded;
  }

  public TxnBatchID getTransactionBatchID() {
    return this.batchID;
  }

  public synchronized boolean isEmpty() {
    return transactionData.isEmpty();
  }

  public synchronized int numberOfTxnsBeforeFolding() {
    return numTxns;
  }

  public synchronized int byteSize() {
    return bytesWritten;
  }

  public boolean isNull() {
    return false;
  }

  public synchronized void removeTransaction(TransactionID txID) {
    TransactionBuffer removed = (TransactionBuffer) transactionData.remove(txID);
    if (removed == null) throw new AssertionError("Attempt to remove a transaction that doesn't exist");
    // if we get some ACKs from the previous instance of the server after we resend this
    // transaction, but before we write to the network, then we don't recycle. We lose those
    // buffers. But since it is a rare scenario we don't lose much, but this check avoid writing
    // corrupt buffers.
    if (outstandingWriteCount == 0) removed.recycle();
  }

  private TransactionBuffer getOrCreateBuffer(ClientTransaction txn, SequenceGenerator sequenceGenerator) {
    if (shouldScanForFold(txn)) {
      for (Iterator i = transactionData.values().iterator(); i.hasNext();) {
        TransactionBuffer txnBuffer = (TransactionBuffer) i.next();
        if (txnBuffer.canAcceptFold(txn)) {
          numFolded++;
          return txnBuffer;
        }
      }
    }

    SequenceID sid = new SequenceID(sequenceGenerator.getNextSequence());
    txn.setSequenceID(sid);

    FoldingKey key = foldingEnabled ? new FoldingKeyImpl(txn.getTransactionType(), txn.getAllLockIDs(), txn
        .getChangeBuffers().keySet()) : DISABLED_FOLDING_KEY;
    TransactionBuffer txnBuffer = new TransactionBuffer(txn.getSequenceID(), newOutputStream(), key, serializer,
                                                        encoding);
    transactionData.put(txn.getTransactionID(), txnBuffer);

    return txnBuffer;
  }

  private boolean shouldScanForFold(ClientTransaction txn) {
    if (!foldingEnabled) { return false; }
    if (foldingLockLimit > 0 && (txn.getAllLockIDs().size() > foldingLockLimit)) { return false; }
    if (foldingObjectLimit > 0 && (txn.getChangeBuffers().size() > foldingObjectLimit)) { return false; }
    return txn.getNewRoots().isEmpty() && txn.getDmiDescriptors().isEmpty() && (txn.getNotifies().isEmpty());
  }

  public synchronized boolean addTransaction(ClientTransaction txn, SequenceGenerator sequenceGenerator) {
    numTxns++;

    TransactionBuffer txnBuffer = getOrCreateBuffer(txn, sequenceGenerator);

    bytesWritten += txnBuffer.write(txn);

    return txnBuffer.getTxnCount() > 1;
  }

  // Called from CommitTransactionMessageImpl
  public synchronized TCByteBuffer[] getData() {
    outstandingWriteCount++;
    TCByteBufferOutputStream out = newOutputStream();
    writeHeader(out);
    for (Iterator i = transactionData.values().iterator(); i.hasNext();) {
      TransactionBuffer tb = ((TransactionBuffer) i.next());
      tb.writeTo(out);
    }
    batchDataOutputStreams.add(out);

    // System.err.println("Batch size: " + out.getBytesWritten() + ", # TXNs = " + numberOfTxns());

    return out.toArray();
  }

  private void writeHeader(TCByteBufferOutputStream out) {
    out.writeLong(this.batchID.toLong());
    out.writeInt(transactionData.size());
  }

  private TCByteBufferOutputStream newOutputStream() {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream(32, 4096, false);
    return out;
  }

  public synchronized void send() {
    CommitTransactionMessage msg = this.commitTransactionMessageFactory.newCommitTransactionMessage();
    msg.setBatch(this, serializer);
    msg.send();
  }

  public synchronized Collection addTransactionIDsTo(Collection c) {
    c.addAll(transactionData.keySet());
    return c;
  }

  public synchronized SequenceID getMinTransactionSequence() {
    return transactionData.isEmpty() ? SequenceID.NULL_ID : ((TransactionBuffer) transactionData.values().iterator()
        .next()).getSequenceID();
  }

  public Collection addTransactionSequenceIDsTo(Collection sequenceIDs) {
    for (Iterator i = transactionData.values().iterator(); i.hasNext();) {
      TransactionBuffer tb = ((TransactionBuffer) i.next());
      sequenceIDs.add(tb.getSequenceID());
    }
    return sequenceIDs;
  }

  // Called from CommitTransactionMessageImpl recycle on write.
  public synchronized void recycle() {
    for (Iterator iter = batchDataOutputStreams.iterator(); iter.hasNext();) {
      TCByteBufferOutputStream buffer = (TCByteBufferOutputStream) iter.next();
      buffer.recycle();
    }
    batchDataOutputStreams.clear();
    outstandingWriteCount--;
  }

  public synchronized String dump() {
    StringBuffer sb = new StringBuffer("TransactionBatchWriter = { \n");
    for (Iterator i = transactionData.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      sb.append(entry.getKey()).append(" = ");
      sb.append(((TransactionBuffer) entry.getValue()).dump());
      sb.append("\n");
    }
    return sb.append(" } ").toString();
  }

  private static final class TransactionBuffer implements Recyclable {

    private static final int               UNINITIALIZED_LENGTH = -1;

    private final FoldingKey               foldingKey;
    private final SequenceID               sequenceID;
    private final TCByteBufferOutputStream output;
    private final ObjectStringSerializer   serializer;
    private final DNAEncoding              encoding;
    private final Mark                     startMark;

    // Maintaining hard references so that it doesn't get GC'ed on us
    private final IdentityHashMap          references           = new IdentityHashMap();
    private final Map                      writers              = new LinkedHashMap();

    private boolean                        needsCopy            = false;
    private int                            headerLength         = UNINITIALIZED_LENGTH;
    private int                            changeCount          = 0;
    private int                            txnCount             = 0;
    private Mark                           changesCountMark;
    private Mark                           txnCountMark;

    TransactionBuffer(SequenceID sequenceID, TCByteBufferOutputStream output, FoldingKey foldingKey,
                      ObjectStringSerializer serializer, DNAEncoding encoding) {
      this.sequenceID = sequenceID;
      this.output = output;
      this.foldingKey = foldingKey;
      this.serializer = serializer;
      this.encoding = encoding;
      this.startMark = output.mark();
    }

    public void writeTo(TCByteBufferOutputStream dest) {
      // XXX: make a writeInt() and writeLong() methods on Mark. Maybe ones that take offsets too

      txnCountMark.write(Conversion.int2Bytes(txnCount));
      changesCountMark.write(Conversion.int2Bytes(changeCount));

      for (Iterator i = writers.values().iterator(); i.hasNext();) {
        DNAWriter writer = (DNAWriter) i.next();
        writer.finalizeHeader();
      }

      if (!needsCopy) {
        dest.write(output.toArray());
        return;
      }

      final int expect = output.getBytesWritten();
      final int begin = dest.getBytesWritten();

      startMark.copyTo(dest, headerLength);
      for (Iterator i = writers.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Entry) i.next();
        DNAWriter writer = (DNAWriter) entry.getValue();

        writer.copyTo(dest);
      }

      Assert.assertEquals(expect, dest.getBytesWritten() - begin);
    }

    boolean canAcceptFold(ClientTransaction txn) {
      return foldingKey.canFold(txn);
    }

    String dump() {
      return " { " + sequenceID + " , Objects in Txn = " + references.size() + " }";
    }

    SequenceID getSequenceID() {
      return this.sequenceID;
    }

    int write(ClientTransaction txn) {
      for (Iterator i = txn.getReferencesOfObjectsInTxn().iterator(); i.hasNext();) {
        this.references.put(i.next(), null);
      }

      int start = output.getBytesWritten();

      if (txnCount == 0) {
        writeFirst(txn);
      } else {
        appendChanges(txn);
      }

      txnCount++;

      return output.getBytesWritten() - start;
    }

    private void appendChanges(ClientTransaction txn) {
      writeChanges(txn.getChangeBuffers());
    }

    private void writeChanges(Map changes) {
      for (Iterator i = changes.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Entry) i.next();
        ObjectID oid = (ObjectID) entry.getKey();
        TCChangeBuffer buffer = (TCChangeBuffer) entry.getValue();

        DNAWriter writer = (DNAWriter) writers.get(oid);
        if (writer == null) {
          TCClass tcc = buffer.getTCObject().getTCClass();
          writer = new DNAWriterImpl(output, oid, tcc.getExtendingClassName(), serializer, encoding, tcc
              .getDefiningLoaderDescription());
          writers.put(oid, writer);
          changeCount++;
        } else {
          writer = writer.createAppender();
        }

        buffer.writeTo(writer);

        if (!writer.isContiguous()) {
          needsCopy = true;
        }
      }
    }

    private void writeFirst(ClientTransaction txn) {
      int startPos = output.getBytesWritten();

      // /////////////////////////////////////////////////////////////////////////////////////////
      // If you're modifying this format, you'll need to update
      // TransactionBatchReader as well
      // /////////////////////////////////////////////////////////////////////////////////////////

      output.writeLong(txn.getTransactionID().toLong());
      output.writeByte(txn.getTransactionType().getType());
      txnCountMark = output.mark();
      output.writeInt(UNINITIALIZED_LENGTH);
      SequenceID sid = txn.getSequenceID();
      if (sid.isNull()) throw new AssertionError("SequenceID is null: " + txn);
      output.writeLong(sid.toLong());

      Collection locks = txn.getAllLockIDs();
      output.writeInt(locks.size());
      for (Iterator i = locks.iterator(); i.hasNext();) {
        output.writeString(((LockID) i.next()).asString());
      }

      Map newRoots = txn.getNewRoots();
      output.writeInt(newRoots.size());
      for (Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
        Entry entry = (Entry) i.next();
        String name = (String) entry.getKey();
        ObjectID id = (ObjectID) entry.getValue();
        output.writeString(name);
        output.writeLong(id.toLong());
      }

      List notifies = txn.getNotifies();
      output.writeInt(notifies.size());
      for (Iterator i = notifies.iterator(); i.hasNext();) {
        Notify n = (Notify) i.next();
        n.serializeTo(output);
      }

      List dmis = txn.getDmiDescriptors();
      output.writeInt(dmis.size());
      for (Iterator i = dmis.iterator(); i.hasNext();) {
        DmiDescriptor dd = (DmiDescriptor) i.next();
        dd.serializeTo(output);
      }

      Map changes = txn.getChangeBuffers();
      this.changesCountMark = output.mark();
      output.writeInt(-1);

      Assert.assertEquals(UNINITIALIZED_LENGTH, headerLength);
      this.headerLength = output.getBytesWritten() - startPos;

      writeChanges(changes);
    }

    int getTxnCount() {
      return txnCount;
    }

    public void recycle() {
      output.recycle();
    }
  }

  private static interface FoldingKey {
    boolean canFold(ClientTransaction txn);
  }

  private static class DisabledFoldingKey implements FoldingKey {
    public boolean canFold(ClientTransaction txn) {
      return false;
    }
  }

  private static class FoldingKeyImpl implements FoldingKey {
    private final List    lockIDs;
    private final Set     objectIDs;
    private final TxnType txnType;

    FoldingKeyImpl(TxnType txnType, List lockIDs, Set objectIDs) {
      this.txnType = txnType;
      this.lockIDs = lockIDs;
      this.objectIDs = objectIDs;
    }

    public boolean canFold(ClientTransaction txn) {
      List txnLocks = txn.getAllLockIDs();
      if (lockIDs.size() != txnLocks.size()) { return false; }

      if (!txn.getTransactionType().equals(txnType)) { return false; }

      Set txnObjectIDs = txn.getChangeBuffers().keySet();

      return lockIDs.equals(txnLocks) && txnObjectIDs.containsAll(objectIDs);
    }
  }

}
