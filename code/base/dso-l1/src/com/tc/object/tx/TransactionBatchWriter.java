/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import org.apache.commons.collections.CollectionUtils;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.lang.Recyclable;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
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
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class TransactionBatchWriter implements ClientTransactionBatch {
  private static final boolean                  DEBUG                     = TCPropertiesImpl.getProperties()
                                                                              .getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_DEBUG);

  private static final TCLogger                 logger                    = TCLogging
                                                                              .getLogger(TransactionBatchWriter.class);

  private final CommitTransactionMessageFactory commitTransactionMessageFactory;
  private final TxnBatchID                      batchID;
  private final LinkedHashMap                   transactionData           = new LinkedHashMap();
  private final Map                             foldingKeys               = new HashMap();
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

  private TransactionBuffer getOrCreateBuffer(ClientTransaction txn, SequenceGenerator sequenceGenerator) {
    if (foldingEnabled) {
      final boolean exceedsLimits = exceedsLimits(txn);

      // txns that exceed the lock/object limits, or those with roots, DMI, and/or notify/notifyAll() cannot be folded
      // and must close any earlier dependent txns
      final boolean scanForClose = (txn.getNewRoots().size() > 0) || (txn.getDmiDescriptors().size() > 0)
                                   || (txn.getNotifies().size() > 0) || exceedsLimits;

      if (DEBUG) log_incomingTxn(txn, exceedsLimits, scanForClose);

      if (scanForClose) {
        scanForClose(txn);
      } else {
        boolean dependencyFound = false;
        FoldingKey potential = null;
        IdentityHashMap dependentKeys = null;

        for (Iterator i = txn.getChangeBuffers().values().iterator(); i.hasNext();) {
          TCChangeBuffer changeBuffer = (TCChangeBuffer) i.next();
          TCObject tco = changeBuffer.getTCObject();
          if (tco.isNew()) {
            if (DEBUG) logger.info("isNew for " + tco.getObjectID());
            continue;
          }

          final ObjectID oid = tco.getObjectID();
          final FoldingKey key = (FoldingKey) foldingKeys.get(oid);
          if (key == null) {
            if (DEBUG) logger.info("no fold key for " + oid);
            continue;
          }

          if (potential == null) {
            if (DEBUG) logger.info("setting potential key to " + System.identityHashCode(key) + " on " + oid);
            potential = key;
            continue;
          }

          if (dependencyFound || potential != key || potential.isClosed()) {
            if (!dependencyFound) {
              if (DEBUG) logger.info("dependency for " + oid + ", potential(" + System.identityHashCode(potential)
                                     + "), key(" + System.identityHashCode(key) + "), potential.closed="
                                     + potential.isClosed());
            }

            if (dependentKeys == null) {
              Assert.assertFalse(dependencyFound);
              dependentKeys = new IdentityHashMap();
              if (DEBUG) logger.info("add " + System.identityHashCode(potential) + " to depKey set on " + oid);
              dependentKeys.put(potential, null);
            }

            dependencyFound = true;

            if (DEBUG) logger.info("add " + System.identityHashCode(key) + " to depKey set on " + oid);
            dependentKeys.put(key, null);
          }
        }

        if (dependencyFound) {
          if (DEBUG) logger.info("Dependency found -- closing dependent keys");
          closeDependentKeys(dependentKeys.keySet());
        } else if (!exceedsLimits && potential != null) {
          if (DEBUG) logger.info("potential fold found " + System.identityHashCode(potential));
          if (potential.canAcceptFold(txn.getAllLockIDs(), txn.getLockType())) {
            if (DEBUG) logger.info("fold accepted into " + System.identityHashCode(potential));

            // need to take on the incoming ObjectIDs present in the buffer we are folding into here
            Set incomingOids = txn.getChangeBuffers().keySet();
            potential.getObjectIDs().addAll(incomingOids);

            registerKeyForOids(incomingOids, potential);

            return potential.getBuffer();
          } else {
            if (DEBUG) logger.info("fold denied into " + System.identityHashCode(potential));
          }
        }
      }
    }

    // if we are here, we are not folding
    SequenceID sid = new SequenceID(sequenceGenerator.getNextSequence());
    txn.setSequenceID(sid);
    if (DEBUG) logger.info("NOT folding, created new sequence " + sid);

    TransactionBuffer txnBuffer = new TransactionBuffer(sid, newOutputStream(), serializer, encoding);

    if (foldingEnabled) {
      FoldingKey key = new FoldingKey(txnBuffer, txn.getLockType(), txn.getAllLockIDs(), new HashSet(txn
          .getChangeBuffers().keySet()));
      registerKeyForOids(txn.getChangeBuffers().keySet(), key);
    }

    transactionData.put(txn.getTransactionID(), txnBuffer);

    return txnBuffer;
  }

  private void registerKeyForOids(Set oids, FoldingKey key) {
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      Object prev = foldingKeys.put(oid, key);
      if (DEBUG) logger.info("registered key(" + System.identityHashCode(key) + " for " + oid + ", replaces key("
                             + System.identityHashCode(prev) + ")");
    }
  }

  private void log_incomingTxn(ClientTransaction txn, boolean exceedsLimits, boolean scanForClose) {
    logger.info("incoming txn [" + txn.getTransactionID() + " locks=" + txn.getAllLockIDs() + ", oids="
                + txn.getChangeBuffers().keySet() + ", dmi=" + txn.getDmiDescriptors() + ", roots=" + txn.getNewRoots()
                + ", notifies=" + txn.getNotifies() + ", type=" + txn.getLockType() + "] exceedsLimit="
                + exceedsLimits + ", scanForClose=" + scanForClose);
  }

  private void closeDependentKeys(Collection dependentKeys) {
    for (Iterator i = dependentKeys.iterator(); i.hasNext();) {
      FoldingKey key = (FoldingKey) i.next();
      if (DEBUG) logger.info("closing dependent key " + System.identityHashCode(key));
      key.close();
    }
  }

  private boolean exceedsLimits(ClientTransaction txn) {
    return exceedsLimit(foldingLockLimit, txn.getAllLockIDs().size())
           || exceedsLimit(foldingObjectLimit, txn.getChangeBuffers().size());
  }

  private void scanForClose(ClientTransaction txn) {
    Collection locks = new HashSet(txn.getAllLockIDs()); // XXX: only create set if needed?
    Set oids = txn.getChangeBuffers().keySet();

    for (Iterator i = foldingKeys.values().iterator(); i.hasNext();) {
      FoldingKey key = (FoldingKey) i.next();
      if (key.isClosed() || key.hasCommonality(locks, oids)) {
        i.remove();
        key.close();
      }
    }
  }

  private static boolean exceedsLimit(int limit, int value) {
    if (limit > 0) { return value > limit; }
    return false;
  }

  public synchronized boolean addTransaction(ClientTransaction txn, SequenceGenerator sequenceGenerator) {
    numTxns++;

    removeEmptyDeltaDna(txn);

    TransactionBuffer txnBuffer = getOrCreateBuffer(txn, sequenceGenerator);

    bytesWritten += txnBuffer.write(txn);

    return txnBuffer.getTxnCount() > 1;
  }

  private void removeEmptyDeltaDna(ClientTransaction txn) {
    for (Iterator i = txn.getChangeBuffers().entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      TCChangeBuffer buffer = (TCChangeBuffer) entry.getValue();
      if ((!buffer.getTCObject().isNew()) && buffer.isEmpty()) {
        i.remove();
      }
    }
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
      output.writeByte(txn.getLockType().getType());
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

  private static class FoldingKey {
    private final List              lockIDs;
    private final Set               objectIDs;
    private final TxnType           txnType;
    private final TransactionBuffer buffer;
    private boolean                 closed;

    FoldingKey(TransactionBuffer buffer, TxnType txnType, List lockIDs, Set objectIDs) {
      this.buffer = buffer;
      this.txnType = txnType;
      this.lockIDs = lockIDs;
      this.objectIDs = objectIDs;

      if (DEBUG) {
        logger.info("created new fold key(" + System.identityHashCode(this) + "), locks=" + lockIDs + ", txnType="
                    + txnType + ", oids=" + objectIDs);
      }
    }

    Set getObjectIDs() {
      return objectIDs;
    }

    public void close() {
      closed = true;
    }

    public boolean isClosed() {
      return closed;
    }

    public boolean hasCommonality(Collection locks, Collection oids) {
      // XXX: Take a lock list here, and only upgrade the larger collection to a set if size > 3
      return CollectionUtils.containsAny(new HashSet(lockIDs), locks) || CollectionUtils.containsAny(objectIDs, oids);
    }

    public TransactionBuffer getBuffer() {
      return this.buffer;
    }

    public boolean canAcceptFold(List txnLocks, TxnType type) {
      if (lockIDs.size() != txnLocks.size()) {
        if (DEBUG) logger.info(System.identityHashCode(this)
                               + ": not accepting fold since lock lists are not equal size");
        return false;
      }

      if (!type.equals(txnType)) {
        if (DEBUG) logger.info(System.identityHashCode(this) + ": not accepting fold since txn type is different");
        return false;
      }

      if (!lockIDs.equals(txnLocks)) {
        if (DEBUG) logger.info(System.identityHashCode(this) + ": not accepting fold since locks lists are not equal");
        return false;
      }

      if (DEBUG) logger.info(System.identityHashCode(this) + ": fold accepted");
      return true;
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
      return new FoldingConfig(props.getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_ENABLED), props.getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT), props
          .getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT));
    }
  }

}
