/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.lang.Recyclable;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggingService;
import com.tc.net.GroupID;
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
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
import com.tc.util.ServiceUtil;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClientTransactionBatchWriter implements ClientTransactionBatch {


  private static final TCLogger                 logger                 = ServiceUtil
                                                                           .loadService(TCLoggingService.class)
                                                                           .getLogger(ClientTransactionBatchWriter.class);

  private final GroupID                         groupID;
  private final CommitTransactionMessageFactory commitTransactionMessageFactory;
  private final TxnBatchID                      batchID;
  private final LinkedHashMap<TransactionID, TransactionBuffer> transactionData = new LinkedHashMap<TransactionID, TransactionBuffer>();
  private final Map<ObjectID, FoldingKey>       foldingKeys            = new HashMap<ObjectID, FoldingKey>();
  private final ObjectStringSerializer          serializer;
  private final DNAEncodingInternal             encoding;
  private final List<TCByteBufferOutputStream>  batchDataOutputStreams = new ArrayList<TCByteBufferOutputStream>();

  private final int                             foldingObjectLimit;
  private final int                             foldingLockLimit;
  private final boolean                         foldingEnabled;
  private final boolean                         debug;

  private short                                 outstandingWriteCount  = 0;
  private int                                   bytesWritten           = 0;
  private int                                   numTxnsBeforeFolding   = 0;
  private int                                   numTxnsAfterFolding    = 0;
  private boolean                               containsSyncWriteTxn   = false;
  private boolean                               committed              = false;
  private int                                   holders                = 0;

  public ClientTransactionBatchWriter(final GroupID groupID, final TxnBatchID batchID,
                                      final ObjectStringSerializer serializer, final DNAEncodingInternal encoding,
                                      final CommitTransactionMessageFactory commitTransactionMessageFactory,
                                      final FoldingConfig foldingConfig) {
    this.groupID = groupID;
    this.batchID = batchID;
    this.encoding = encoding;
    this.commitTransactionMessageFactory = commitTransactionMessageFactory;
    this.serializer = serializer;

    this.foldingLockLimit = foldingConfig.getLockLimit();
    this.foldingObjectLimit = foldingConfig.getObjectLimit();
    this.foldingEnabled = foldingConfig.isFoldingEnabled();
    this.debug = foldingConfig.isDebugLogging();
  }

  @Override
  public synchronized String toString() {
    return super.toString() + "[" + this.batchID + ", isEmpty=" + isEmpty() + ", numTxnsBeforeFolding= "
           + this.numTxnsBeforeFolding + " numTxnAfterfoldingTxn= " + this.numTxnsAfterFolding + " size="
           + this.bytesWritten + " foldingKeys=" + this.foldingKeys.size();
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
  public synchronized int numberOfTxnsBeforeFolding() {
    return this.numTxnsBeforeFolding;
  }

  @Override
  public synchronized int byteSize() {
    return this.bytesWritten;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public synchronized TransactionBuffer removeTransaction(final TransactionID txID) {
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
  public synchronized boolean contains(final TransactionID txID) {
    return this.transactionData.containsKey(txID);
  }

  private TransactionBuffer createBuffer(final ClientTransaction txn) {

    final TransactionBuffer txnBuffer = createTransactionBuffer(txn.getSequenceID(), newOutputStream(),
                                                                this.serializer, this.encoding, txn.getTransactionID());

    this.transactionData.put(txn.getTransactionID(), txnBuffer);

    return txnBuffer;
  }

  private TransactionBuffer getOrCreateBuffer(final ClientTransaction txn, final SequenceGenerator sequenceGenerator,
                                              final TransactionIDGenerator tidGenerator) {
    final boolean exceedsLimits = exceedsLimits(txn);

    // txns that exceed the lock/object limits, or those with roots, and/or notify/notifyAll() cannot be folded
    // and must close any earlier dependent txns
    final boolean scanForClose = (txn.getNewRoots().size() > 0) || (txn.getNotifies().size() > 0) || exceedsLimits;

    if (debug) {
      log_incomingTxn(txn, exceedsLimits, scanForClose);
    }

    if (scanForClose) {
      scanForClose(txn);
    } else {
      boolean dependencyFound = false;
      FoldingKey potential = null;
      IdentityHashMap<FoldingKey, Object> dependentKeys = null;

      for (TCChangeBuffer changeBuffer : txn.getChangeBuffers().values()) {
        final TCObject tco = changeBuffer.getTCObject();
        if (tco.isNew()) {
          if (debug) {
            logger.info("isNew for " + tco.getObjectID());
          }
          continue;
        }

        final ObjectID oid = tco.getObjectID();
        final FoldingKey key = this.foldingKeys.get(oid);
        if (key == null) {
          if (debug) {
            logger.info("no fold key for " + oid);
          }
          continue;
        }

        if (potential == null) {
          if (debug) {
            logger.info("setting potential key to " + System.identityHashCode(key) + " on " + oid);
          }
          potential = key;
          continue;
        }

        if (dependencyFound || potential != key || potential.isClosed()) {
          if (!dependencyFound) {
            if (debug) {
              logger.info("dependency for " + oid + ", potential(" + System.identityHashCode(potential) + "), key("
                          + System.identityHashCode(key) + "), potential.closed=" + potential.isClosed());
            }
          }

          if (dependentKeys == null) {
            Assert.assertFalse(dependencyFound);
            dependentKeys = new IdentityHashMap<FoldingKey, Object>();
            if (debug) {
              logger.info("add " + System.identityHashCode(potential) + " to depKey set on " + oid);
            }
            dependentKeys.put(potential, null);
          }

          dependencyFound = true;

          if (debug) {
            logger.info("add " + System.identityHashCode(key) + " to depKey set on " + oid);
          }
          dependentKeys.put(key, null);
        }
      }

      if (dependencyFound) {
        if (debug) {
          logger.info("Dependency found -- closing dependent keys");
        }
        closeDependentKeys(dependentKeys.keySet());
      } else if (!exceedsLimits && potential != null) {
        if (debug) {
          logger.info("potential fold found " + System.identityHashCode(potential));
        }
        if (potential.canAcceptFold(txn.getAllLockIDs(), txn.getLockType(), debug)) {
          if (debug) {
            logger.info("fold accepted into " + System.identityHashCode(potential));
          }

          // need to take on the incoming ObjectIDs present in the buffer we are folding into here
          final Set<ObjectID> incomingOids = txn.getChangeBuffers().keySet();
          potential.getObjectIDs().addAll(incomingOids);

          registerKeyForOids(incomingOids, potential);

          return potential.getBuffer();
        } else {
          if (debug) {
            logger.info("fold denied into " + System.identityHashCode(potential));
          }
        }
      }
    }

    // if we are here, we are not folding
    final SequenceID sid = new SequenceID(sequenceGenerator.getNextSequence());
    txn.setSequenceID(sid);
    txn.setTransactionID(tidGenerator.nextTransactionID());
    if (debug) {
      logger.info("NOT folding, created new sequence " + sid);
    }

    final TransactionBuffer txnBuffer = createTransactionBuffer(sid, newOutputStream(), this.serializer, this.encoding,
                                                                txn.getTransactionID());

    final FoldingKey key = new FoldingKey(txnBuffer, txn.getLockType(), new HashSet<ObjectID>(txn.getChangeBuffers().keySet()),
                                          debug);
    ++this.numTxnsAfterFolding;
    registerKeyForOids(txn.getChangeBuffers().keySet(), key);

    this.transactionData.put(txn.getTransactionID(), txnBuffer);

    return txnBuffer;
  }

  // Overridden in active-active
  protected TransactionBuffer createTransactionBuffer(final SequenceID sid,
                                                      final TCByteBufferOutputStream newOutputStream,
                                                      final ObjectStringSerializer objectStringserializer,
                                                      final DNAEncodingInternal dnaEncoding, final TransactionID txnID) {
    return new TransactionBufferImpl(this, sid, newOutputStream, objectStringserializer, dnaEncoding, txnID);
  }

  private void registerKeyForOids(final Set<ObjectID> oids, final FoldingKey key) {
    for (final ObjectID oid : oids) {
      final Object prev = this.foldingKeys.put(oid, key);
      if (debug) {
        logger.info("registered key(" + System.identityHashCode(key) + " for " + oid + ", replaces key("
                    + System.identityHashCode(prev) + ")");
      }
    }
  }

  private void log_incomingTxn(final ClientTransaction txn, final boolean exceedsLimits, final boolean scanForClose) {
    logger.info("incoming txn@" + System.identityHashCode(txn) + "[" + txn.getTransactionID() + " locks="
                + txn.getAllLockIDs() + ", oids=" + txn.getChangeBuffers().keySet() + ", roots=" + txn.getNewRoots()
                + ", notifies=" + txn.getNotifies() + ", type=" + txn.getLockType() + "] exceedsLimit=" + exceedsLimits
                + ", scanForClose=" + scanForClose);
  }

  private void closeDependentKeys(final Collection<FoldingKey> dependentKeys) {
    for (FoldingKey key : dependentKeys) {
      if (debug) {
        logger.info("closing dependent key " + System.identityHashCode(key));
      }
      key.close();
    }
  }

  private boolean exceedsLimits(final ClientTransaction txn) {
    return exceedsLimit(this.foldingLockLimit, txn.getAllLockIDs().size())
           || exceedsLimit(this.foldingObjectLimit, txn.getChangeBuffers().size());
  }

  private void scanForClose(final ClientTransaction txn) {
    final Collection<LockID> locks = new HashSet<LockID>(txn.getAllLockIDs()); // XXX: only create set if needed?
    final Set<ObjectID> oids = txn.getChangeBuffers().keySet();

    for (final Iterator<FoldingKey> i = this.foldingKeys.values().iterator(); i.hasNext();) {
      final FoldingKey key = i.next();
      if (key.isClosed() || key.hasCommonality(locks, oids)) {
        i.remove();
        key.close();
      }
    }
  }

  private static boolean exceedsLimit(final int limit, final int value) {
    if (limit > 0) { return value > limit; }
    return false;
  }

  @Override
  public synchronized FoldedInfo addTransaction(final ClientTransaction txn, final SequenceGenerator sequenceGenerator,
                                                final TransactionIDGenerator tidGenerator) {
    holders += 1;
    TransactionBuffer txnBuffer = null;
    if (!foldingEnabled) {
      txn.setSequenceID(new SequenceID(sequenceGenerator.getNextSequence()));
      txn.setTransactionID(tidGenerator.nextTransactionID());
      txnBuffer = addSimpleTransaction(txn);
    } else {
      if (committed) { throw new AssertionError("Already committed"); }

      this.numTxnsBeforeFolding++;

      if (txn.getLockType().equals(TxnType.SYNC_WRITE)) {
        this.containsSyncWriteTxn = true;
      }

      removeEmptyDeltaDna(txn);

      txnBuffer = getOrCreateBuffer(txn, sequenceGenerator, tidGenerator);
      txnBuffer.addTransactionCompleteListeners(txn.getTransactionCompleteListeners());
    }

    this.bytesWritten += txnBuffer.write(txn);
    return new FoldedInfo(txnBuffer);
  }

  @Override
  public synchronized TransactionBuffer addSimpleTransaction(final ClientTransaction txn) {
    holders += 1;

    if (committed) { throw new AssertionError("Already committed"); }

    this.numTxnsBeforeFolding++;

    if (txn.getLockType().equals(TxnType.SYNC_WRITE)) {
      this.containsSyncWriteTxn = true;
    }

    removeEmptyDeltaDna(txn);

    final TransactionBuffer txnBuffer = createBuffer(txn);

    txnBuffer.addTransactionCompleteListeners(txn.getTransactionCompleteListeners());

    return txnBuffer;
  }

  private synchronized void release() {
    if (--holders == 0) {
      notify();
    }
  }

  private void removeEmptyDeltaDna(final ClientTransaction txn) {
    for (final Iterator<Entry<ObjectID, TCChangeBuffer>> i = txn.getChangeBuffers().entrySet().iterator(); i.hasNext();) {
      final Entry<ObjectID, TCChangeBuffer> entry = i.next();
      final TCChangeBuffer buffer = entry.getValue();
      if ((!buffer.getTCObject().isNew()) && buffer.isEmpty()) {
        i.remove();
      }
    }
  }

  @SuppressWarnings("resource")
  @Override
  public synchronized TCByteBuffer[] getData() {
    this.committed = true;
    this.foldingKeys.clear();

    this.outstandingWriteCount++;
    final TCByteBufferOutputStream out = newOutputStream();
    writeHeader(out);
    for (TransactionBuffer tb : this.transactionData.values()) {
      tb.writeTo(out);
    }
    this.batchDataOutputStreams.add(out);

    return out.toArray();
  }

  protected void writeHeader(final TCByteBufferOutputStream out) {
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
      msg = this.commitTransactionMessageFactory.newCommitTransactionMessage(this.groupID);
      msg.setBatch(this, this.serializer);
    }
    msg.send();
  }

  @Override
  public synchronized Collection<TransactionID> addTransactionIDsTo(final Collection<TransactionID> c) {
    c.addAll(this.transactionData.keySet());
    return c;
  }

  @Override
  public synchronized SequenceID getMinTransactionSequence() {
    return this.transactionData.isEmpty() ? SequenceID.NULL_ID : ((TransactionBufferImpl) this.transactionData.values()
        .iterator().next()).getSequenceID();
  }

  @Override
  public Collection<SequenceID> addTransactionSequenceIDsTo(final Collection<SequenceID> sequenceIDs) {
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

    private static final int               UNINITIALIZED_LENGTH = -1;

    private final ClientTransactionBatchWriter parent;
    private final SequenceID               sequenceID;
    private final TCByteBufferOutputStream output;
    private final ObjectStringSerializer   serializer;
    private final DNAEncodingInternal      encoding;
    private final Mark                     startMark;
    private final SetOnceFlag              committed            = new SetOnceFlag();

    private final Map<ObjectID, DNAWriter> writers              = new LinkedHashMap<ObjectID, DNAWriter>();
    private final TransactionID            txnID;

    // Maintaining hard references so that it doesn't get GC'ed on us
    private final IdentityHashMap<Object, Object> references    = new IdentityHashMap<Object, Object>();
    private boolean                        needsCopy            = false;
    private int                            headerLength         = UNINITIALIZED_LENGTH;
    private int                            txnCount             = 0;
    private Mark                           changesCountMark;
    private Mark                           txnCountMark;
    private List<TransactionCompleteListener> txnCompleteListers;
    

    TransactionBufferImpl(final ClientTransactionBatchWriter parent,
                          final SequenceID sequenceID, final TCByteBufferOutputStream output,
                          final ObjectStringSerializer serializer, final DNAEncodingInternal encoding,
                          final TransactionID txnID) {
      this.parent = parent;
      this.sequenceID = sequenceID;
      this.output = output;
      this.serializer = serializer;
      this.encoding = encoding;
      this.startMark = output.mark();
      this.txnID = txnID;
    }

    @Override
    public TransactionID getFoldedTransactionID() {
      return this.txnID;
    }

    @Override
    public void writeTo(final TCByteBufferOutputStream dest) {
      // XXX: make a writeInt() and writeLong() methods on Mark. Maybe ones that take offsets too

      // This check is needed since this buffer might need to be resent upon server crash
      if (this.committed.attemptSet()) {
        this.txnCountMark.write(Conversion.int2Bytes(this.txnCount));
        this.changesCountMark.write(Conversion.int2Bytes(this.writers.size()));

        for (DNAWriter writer : this.writers.values()) {
          writer.finalizeHeader();
        }
      }

      if (!this.needsCopy) {
        dest.write(this.output.toArray());
        return;
      }

      final int expect = this.output.getBytesWritten();
      final int begin = dest.getBytesWritten();

      this.startMark.copyTo(dest, this.headerLength);
      for (DNAWriter writer : this.writers.values()) {
        writer.copyTo(dest);
      }

      Assert.assertEquals(expect, dest.getBytesWritten() - begin);
    }

    String dump() {
      return " { " + this.sequenceID + " , Txns in Buffer = " + this.references.size()
             + " , Objects in (Folded) Txn : " + this.writers.size() + " }";
    }

    SequenceID getSequenceID() {
      return this.sequenceID;
    }

    @Override
    public int write(final ClientTransaction txn) {
      // Holding on the object references, this method could be called more than once for folded transactions.
      // By definition on the second and subsequent calls will have repeated object references in it, so put() to the
      // map here to not store dupes.
      try {
        for (Object reference : txn.getReferencesOfObjectsInTxn()) {
          this.references.put(reference, null);
        }

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

    private void appendChanges(final ClientTransaction txn) {
      writeChanges(txn.getChangeBuffers());
    }

    private void writeChanges(final Map<ObjectID, TCChangeBuffer> changes) {      
      for (Map.Entry<ObjectID, TCChangeBuffer> entry : changes.entrySet()) {
        final ObjectID oid = entry.getKey();
        final TCChangeBuffer buffer = entry.getValue();

        final TCObject tco = buffer.getTCObject();

        final boolean isNew = tco.isNew();

        DNAWriter writer = this.writers.get(oid);
        if (writer == null) {
          writer = new DNAWriterImpl(this.output, oid, tco.getClassName(), this.serializer, this.encoding,
                                     !isNew);

          this.writers.put(oid, writer);
        } else {
          writer = writer.createAppender();
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

        if (!writer.isContiguous()) {
          this.needsCopy = true;
        }
      }
    }

    private void writeFirst(final ClientTransaction txn) {
      writeTransactionHeader(txn);
      final Map<ObjectID, TCChangeBuffer> changes = txn.getChangeBuffers();
      writeChanges(changes);
    }

    private void writeTransactionHeader(final ClientTransaction txn) {
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
      for (Notify n: notifies) {
        n.serializeTo(this.output);
      }

      writeAdditionalHeaderInformation(this.output, txn);

      this.changesCountMark = this.output.mark();
      this.output.writeInt(-1);

      Assert.assertEquals(UNINITIALIZED_LENGTH, this.headerLength);
      this.headerLength = this.output.getBytesWritten() - startPos;

    }

    protected void writeAdditionalHeaderInformation(final TCByteBufferOutputStream out, final ClientTransaction txn) {
      // This is here so that the format is compatible with active-active
      writeLongArray(out, new long[0]);
    }

    protected void writeLongArray(final TCByteBufferOutputStream out, final long[] ls) {
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

  private static class FoldingKey {
    private final Set<ObjectID>               objectIDs;
    private final TxnType           txnType;
    private final TransactionBuffer buffer;
    private boolean                 closed;

    FoldingKey(final TransactionBuffer txnBuffer, final TxnType txnType, final Set<ObjectID> objectIDs, final boolean debug) {
      this.buffer = txnBuffer;
      this.txnType = txnType;
      this.objectIDs = objectIDs;

      if (debug) {
        logger.info("created new fold key(" + System.identityHashCode(this) + "), txnType=" + txnType + ", oids="
                    + objectIDs);
      }
    }

    Set<ObjectID> getObjectIDs() {
      return this.objectIDs;
    }

    public void close() {
      this.closed = true;
    }

    public boolean isClosed() {
      return this.closed;
    }

    public boolean hasCommonality(final Collection<LockID> locks, final Collection<ObjectID> oids) {
      if (objectIDs.size() > oids.size()) {
        for (ObjectID oid : oids) {
          if (objectIDs.contains(oid)) { return true; }
        }
      } else {
        for (ObjectID oid : objectIDs) {
          if (oids.contains(oid)) { return true; }
        }
      }

      return false;
    }

    public TransactionBuffer getBuffer() {
      return this.buffer;
    }

    public boolean canAcceptFold(final List<LockID> txnLocks, final TxnType type, final boolean debug) {
      // relax folding rules to allow folding txn with common object even lock sets different
      if (!type.equals(this.txnType)) {
        if (debug) {
          logger.info(System.identityHashCode(this) + ": not accepting fold since txn type is different");
        }
        return false;
      }

      if (debug) {
        logger.info(System.identityHashCode(this) + ": fold accepted");
      }

      return true;
    }
  }

  public static class FoldingConfig {
    private final int     lockLimit;
    private final int     objectLimit;
    private final boolean foldingEnabled;
    private final boolean debugLogging;

    public FoldingConfig(final boolean foldingEnabled, final int objectLimit, final int lockLimit,
                         final boolean debugLogging) {
      this.foldingEnabled = foldingEnabled;
      this.objectLimit = objectLimit;
      this.lockLimit = lockLimit;
      this.debugLogging = debugLogging;
    }

    public int getLockLimit() {
      return this.lockLimit;
    }

    public int getObjectLimit() {
      return this.objectLimit;
    }

    public boolean isFoldingEnabled() {
      return this.foldingEnabled;
    }

    public boolean isDebugLogging() {
      return debugLogging;
    }
  }

  public static class FoldedInfo {
    private final TransactionBuffer buffer;
    private final TransactionID     txnID;
    private final boolean           folded;

    public FoldedInfo(final TransactionBuffer buffer) {
      this.buffer = buffer;
      this.txnID = buffer.getFoldedTransactionID();
      this.folded = buffer.getTxnCount() > 1;
    }

    public TransactionID getFoldedTransactionID() {
      return this.txnID;
    }

    public boolean isFolded() {
      return this.folded;
    }

    public TransactionBuffer getBuffer() {
      return this.buffer;
    }
  }

}
