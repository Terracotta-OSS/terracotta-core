/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import org.apache.commons.collections.CollectionUtils;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.lang.Recyclable;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
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
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class TransactionBatchWriter implements ClientTransactionBatch {
  public static final String                    FOLDING_ENABLED_PROP      = "l1.transactionmanager.folding.enabled";
  public static final String                    FOLDING_OBJECT_LIMIT_PROP = "l1.transactionmanager.folding.object.limit";
  public static final String                    FOLDING_LOCK_LIMIT_PROP   = "l1.transactionmanager.folding.lock.limit";

  private final CommitTransactionMessageFactory commitTransactionMessageFactory;
  private final TxnBatchID                      batchID;
  private final LinkedHashMap                   transactionData           = new LinkedHashMap();
  private final LinkedList                      foldingKeys               = new LinkedList();
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
                                CommitTransactionMessageFactory commitTransactionMessageFactory,
                                FoldingConfig foldingConfig) {
    this.batchID = batchID;
    this.encoding = encoding;
    this.commitTransactionMessageFactory = commitTransactionMessageFactory;
    this.serializer = serializer;

    this.foldingEnabled = foldingConfig.isFoldingEnabled();
    this.foldingLockLimit = foldingConfig.getLockLimit();
    this.foldingObjectLimit = foldingConfig.getObjectLimit();
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

  private Set getDeltaOIDs(ClientTransaction txn) {
    Set rv = null;
    for (Iterator i = txn.getChangeBuffers().values().iterator(); i.hasNext();) {
      TCChangeBuffer changeBuffer = (TCChangeBuffer) i.next();
      TCObject tco = changeBuffer.getTCObject();
      if (!tco.isNew()) {
        if (rv == null) {
          rv = new HashSet();
        }
        rv.add(tco.getObjectID());
      }
    }
    return rv == null ? Collections.EMPTY_SET : rv;
  }

  private TransactionBuffer getOrCreateBuffer(ClientTransaction txn, SequenceGenerator sequenceGenerator) {
    if (foldingEnabled) {
      final boolean exceedsLimits = exceedsLimits(txn);

      // txns that exceed the lock/object limits, or those with roots, DMI, and/or notify/notifyAll() cannot be folded
      // and must close any earlier dependent txns
      final boolean scanForClose = (txn.getNewRoots().size() > 0) || (txn.getDmiDescriptors().size() > 0)
                                   || (txn.getNotifies().size() > 0) || exceedsLimits;

      if (scanForClose) {
        scanForClose(txn);
      } else if (!exceedsLimits) {
        final Set deltaOIDs = getDeltaOIDs(txn); // only need to do this once, not for each fold key

        boolean stop = false;
        for (Iterator i = foldingKeys.iterator(); !stop && i.hasNext();) {
          FoldingKey key = (FoldingKey) i.next();

          final byte action = key.analyzeForFold(txn, deltaOIDs);
          switch (action) {
            case FoldingKey.ACCEPT: {
              numFolded++;
              return key.getBuffer();
            }
            case FoldingKey.DENY: {
              break;
            }
            case FoldingKey.CLOSE: {
              i.remove();
              stop = true;
              break;
            }
            default: {
              throw new AssertionError("unknown action: " + action);
            }
          }
        }
      }
    }

    SequenceID sid = new SequenceID(sequenceGenerator.getNextSequence());
    txn.setSequenceID(sid);

    TransactionBuffer txnBuffer = new TransactionBuffer(sid, newOutputStream(), serializer, encoding);

    if (foldingEnabled) {
      foldingKeys.add(new FoldingKeyImpl(txnBuffer, txn.getTransactionType(), txn.getAllLockIDs(), new HashSet(txn
          .getChangeBuffers().keySet())));
    }

    transactionData.put(txn.getTransactionID(), txnBuffer);

    return txnBuffer;
  }

  private boolean exceedsLimits(ClientTransaction txn) {
    return exceedsLimit(foldingLockLimit, txn.getAllLockIDs().size())
           || exceedsLimit(foldingObjectLimit, txn.getChangeBuffers().size());
  }

  private void scanForClose(ClientTransaction txn) {
    List locks = txn.getAllLockIDs();
    Set oids = txn.getChangeBuffers().keySet();

    for (Iterator i = foldingKeys.iterator(); i.hasNext();) {
      FoldingKey key = (FoldingKey) i.next();
      if (key.hasCommonality(locks, oids)) {
        i.remove();
      }
    }
  }

  private static boolean exceedsLimit(int limit, int value) {
    if (limit > 0) { return value > limit; }
    return false;
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

    private final SequenceID               sequenceID;
    private final TCByteBufferOutputStream output;
    private final ObjectStringSerializer   serializer;
    private final DNAEncoding              encoding;
    private final Mark                     startMark;
    private final SetOnceFlag              committed            = new SetOnceFlag();

    // Maintaining hard references so that it doesn't get GC'ed on us
    private final IdentityHashMap          references           = new IdentityHashMap();
    private final Map                      writers              = new LinkedHashMap();

    private boolean                        needsCopy            = false;
    private int                            headerLength         = UNINITIALIZED_LENGTH;
    private int                            txnCount             = 0;
    private Mark                           changesCountMark;
    private Mark                           txnCountMark;

    TransactionBuffer(SequenceID sequenceID, TCByteBufferOutputStream output, ObjectStringSerializer serializer,
                      DNAEncoding encoding) {
      this.sequenceID = sequenceID;
      this.output = output;
      this.serializer = serializer;
      this.encoding = encoding;
      this.startMark = output.mark();
    }

    public void writeTo(TCByteBufferOutputStream dest) {
      // XXX: make a writeInt() and writeLong() methods on Mark. Maybe ones that take offsets too

      // This check is needed since this buffer might need to be resent upon server crash
      if (committed.attemptSet()) {
        txnCountMark.write(Conversion.int2Bytes(txnCount));
        changesCountMark.write(Conversion.int2Bytes(writers.size()));

        for (Iterator i = writers.values().iterator(); i.hasNext();) {
          DNAWriter writer = (DNAWriter) i.next();
          writer.finalizeHeader();
        }
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

        TCObject tco = buffer.getTCObject();

        final boolean isNew = tco.isNew();

        DNAWriter writer = (DNAWriter) writers.get(oid);
        if (writer == null) {
          TCClass tcc = tco.getTCClass();
          writer = new DNAWriterImpl(output, oid, tcc.getExtendingClassName(), serializer, encoding, tcc
              .getDefiningLoaderDescription(), !isNew);

          writers.put(oid, writer);
        } else {
          writer = writer.createAppender();
        }

        // this isNew() check and flipping of the new flag are safe here only because transaction writing is completely
        // serialized within the the batch writer. This ensure there is no race for more than one thread to commit the
        // same "new" object
        if (isNew) {
          tco.dehydrate(writer);
          tco.setNotNew();
        } else {
          buffer.writeTo(writer);
        }

        writer.markSectionEnd();

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

  private interface FoldingKey {
    static final byte ACCEPT = 1;
    static final byte CLOSE  = 2;
    static final byte DENY   = 3;

    byte analyzeForFold(ClientTransaction txn, Set deltaOIDs);

    boolean hasCommonality(List allLockIDs, Set keySet);

    TransactionBuffer getBuffer();
  }

  private static class FoldingKeyImpl implements FoldingKey {
    private final List              lockIDs;
    private final Set               objectIDs;
    private final TxnType           txnType;
    private final TransactionBuffer buffer;

    FoldingKeyImpl(TransactionBuffer buffer, TxnType txnType, List lockIDs, Set objectIDs) {
      this.buffer = buffer;
      this.txnType = txnType;
      this.lockIDs = lockIDs;
      this.objectIDs = objectIDs;
    }

    public boolean hasCommonality(List locks, Set oids) {
      return CollectionUtils.containsAny(lockIDs, locks) || CollectionUtils.containsAny(objectIDs, oids);
    }

    public TransactionBuffer getBuffer() {
      return this.buffer;
    }

    public byte analyzeForFold(ClientTransaction txn, Set deltaOIDs) {
      List txnLocks = txn.getAllLockIDs();

      if (lockIDs.size() != txnLocks.size()) { return chooseCloseOrDeny(deltaOIDs); }

      if (!txn.getTransactionType().equals(txnType)) { return chooseCloseOrDeny(deltaOIDs); }

      if (lockIDs.equals(txnLocks) && CollectionUtils.containsAny(objectIDs, deltaOIDs)) {
        // need to take on the new ObjectIDs present in the buffer we are folding into here
        objectIDs.addAll(txn.getChangeBuffers().keySet());
        return ACCEPT;
      }

      return chooseCloseOrDeny(deltaOIDs);
    }

    private byte chooseCloseOrDeny(Set txnObjectIDs) {
      if (CollectionUtils.containsAny(objectIDs, txnObjectIDs)) { return CLOSE; }
      return DENY;
    }

  }

  public static class FoldingConfig {
    private final int     lockLimit;
    private final int     objectLimit;
    private final boolean foldingEnabled;

    public FoldingConfig(boolean foldingEnabled, int objectLimit, int lockLimit) {
      this.foldingEnabled = foldingEnabled;
      this.objectLimit = objectLimit;
      this.lockLimit = lockLimit;
    }

    public int getLockLimit() {
      return lockLimit;
    }

    public int getObjectLimit() {
      return objectLimit;
    }

    public boolean isFoldingEnabled() {
      return foldingEnabled;
    }

    public static FoldingConfig createFromProperties(TCProperties props) {
      return new FoldingConfig(props.getBoolean(FOLDING_ENABLED_PROP), props.getInt(FOLDING_OBJECT_LIMIT_PROP), props
          .getInt(FOLDING_LOCK_LIMIT_PROP));
    }
  }

}
