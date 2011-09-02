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
import com.tc.net.GroupID;
import com.tc.object.ObjectID;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIDSerializer;
import com.tc.object.locks.Notify;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.SequenceGenerator;
import com.tc.util.SequenceID;
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
  private static final boolean                  DEBUG                  = TCPropertiesImpl
                                                                           .getProperties()
                                                                           .getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_DEBUG);

  private static final TCLogger                 logger                 = TCLogging
                                                                           .getLogger(ClientTransactionBatchWriter.class);

  private final GroupID                         groupID;
  private final CommitTransactionMessageFactory commitTransactionMessageFactory;
  private final TxnBatchID                      batchID;
  private final LinkedHashMap                   transactionData        = new LinkedHashMap();
  private final Map                             foldingKeys            = new HashMap();
  private final ObjectStringSerializer          serializer;
  private final DNAEncodingInternal             encoding;
  private final List                            batchDataOutputStreams = new ArrayList();

  private final boolean                         foldingEnabled;
  private final int                             foldingObjectLimit;
  private final int                             foldingLockLimit;

  private short                                 outstandingWriteCount  = 0;
  private int                                   bytesWritten           = 0;
  private int                                   numTxnsBeforeFolding   = 0;
  private int                                   numTxnsAfterFolding    = 0;
  private boolean                               containsSyncWriteTxn   = false;

  public ClientTransactionBatchWriter(final GroupID groupID, final TxnBatchID batchID,
                                      final ObjectStringSerializer serializer, final DNAEncodingInternal encoding,
                                      final CommitTransactionMessageFactory commitTransactionMessageFactory,
                                      final FoldingConfig foldingConfig) {
    this.groupID = groupID;
    this.batchID = batchID;
    this.encoding = encoding;
    this.commitTransactionMessageFactory = commitTransactionMessageFactory;
    this.serializer = serializer;

    this.foldingEnabled = foldingConfig.isFoldingEnabled();
    this.foldingLockLimit = foldingConfig.getLockLimit();
    this.foldingObjectLimit = foldingConfig.getObjectLimit();
  }

  @Override
  public synchronized String toString() {
    return super.toString() + "[" + this.batchID + ", isEmpty=" + isEmpty() + ", numTxnsBeforeFolding= "
           + this.numTxnsBeforeFolding + " numTxnAfterfoldingTxn= " + this.numTxnsAfterFolding + " size="
           + this.bytesWritten + " foldingKeys=" + this.foldingKeys.size();
  }

  public TxnBatchID getTransactionBatchID() {
    return this.batchID;
  }

  public synchronized boolean isEmpty() {
    return this.transactionData.isEmpty();
  }

  public synchronized int numberOfTxnsBeforeFolding() {
    return this.numTxnsBeforeFolding;
  }

  public synchronized int byteSize() {
    return this.bytesWritten;
  }

  public boolean isNull() {
    return false;
  }

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

  private TransactionBuffer getOrCreateBuffer(final ClientTransaction txn, final SequenceGenerator sequenceGenerator,
                                              final TransactionIDGenerator tidGenerator) {
    if (this.foldingEnabled) {
      final boolean exceedsLimits = exceedsLimits(txn);

      // txns that exceed the lock/object limits, or those with roots, DMI, and/or notify/notifyAll() cannot be folded
      // and must close any earlier dependent txns
      final boolean scanForClose = (txn.getNewRoots().size() > 0) || (txn.getDmiDescriptors().size() > 0)
                                   || (txn.getNotifies().size() > 0) || exceedsLimits;

      if (DEBUG) {
        log_incomingTxn(txn, exceedsLimits, scanForClose);
      }

      if (scanForClose) {
        scanForClose(txn);
      } else {
        boolean dependencyFound = false;
        FoldingKey potential = null;
        IdentityHashMap dependentKeys = null;

        for (final Iterator i = txn.getChangeBuffers().values().iterator(); i.hasNext();) {
          final TCChangeBuffer changeBuffer = (TCChangeBuffer) i.next();
          final TCObject tco = changeBuffer.getTCObject();
          if (tco.isNew()) {
            if (DEBUG) {
              logger.info("isNew for " + tco.getObjectID());
            }
            continue;
          }

          final ObjectID oid = tco.getObjectID();
          final FoldingKey key = (FoldingKey) this.foldingKeys.get(oid);
          if (key == null) {
            if (DEBUG) {
              logger.info("no fold key for " + oid);
            }
            continue;
          }

          if (potential == null) {
            if (DEBUG) {
              logger.info("setting potential key to " + System.identityHashCode(key) + " on " + oid);
            }
            potential = key;
            continue;
          }

          if (dependencyFound || potential != key || potential.isClosed()) {
            if (!dependencyFound) {
              if (DEBUG) {
                logger.info("dependency for " + oid + ", potential(" + System.identityHashCode(potential) + "), key("
                            + System.identityHashCode(key) + "), potential.closed=" + potential.isClosed());
              }
            }

            if (dependentKeys == null) {
              Assert.assertFalse(dependencyFound);
              dependentKeys = new IdentityHashMap();
              if (DEBUG) {
                logger.info("add " + System.identityHashCode(potential) + " to depKey set on " + oid);
              }
              dependentKeys.put(potential, null);
            }

            dependencyFound = true;

            if (DEBUG) {
              logger.info("add " + System.identityHashCode(key) + " to depKey set on " + oid);
            }
            dependentKeys.put(key, null);
          }
        }

        if (dependencyFound) {
          if (DEBUG) {
            logger.info("Dependency found -- closing dependent keys");
          }
          closeDependentKeys(dependentKeys.keySet());
        } else if (!exceedsLimits && potential != null) {
          if (DEBUG) {
            logger.info("potential fold found " + System.identityHashCode(potential));
          }
          if (potential.canAcceptFold(txn.getAllLockIDs(), txn.getLockType())) {
            if (DEBUG) {
              logger.info("fold accepted into " + System.identityHashCode(potential));
            }

            // need to take on the incoming ObjectIDs present in the buffer we are folding into here
            final Set incomingOids = txn.getChangeBuffers().keySet();
            potential.getObjectIDs().addAll(incomingOids);

            registerKeyForOids(incomingOids, potential);

            return potential.getBuffer();
          } else {
            if (DEBUG) {
              logger.info("fold denied into " + System.identityHashCode(potential));
            }
          }
        }
      }
    }

    // if we are here, we are not folding
    final SequenceID sid = new SequenceID(sequenceGenerator.getNextSequence());
    txn.setSequenceID(sid);
    txn.setTransactionID(tidGenerator.nextTransactionID());
    if (DEBUG) {
      logger.info("NOT folding, created new sequence " + sid);
    }

    final TransactionBuffer txnBuffer = createTransactionBuffer(sid, newOutputStream(), this.serializer, this.encoding,
                                                                txn.getTransactionID());

    if (this.foldingEnabled) {

      final FoldingKey key = new FoldingKey(txnBuffer, txn.getLockType(), new HashSet(txn.getChangeBuffers().keySet()));
      ++this.numTxnsAfterFolding;
      registerKeyForOids(txn.getChangeBuffers().keySet(), key);
    }

    this.transactionData.put(txn.getTransactionID(), txnBuffer);

    return txnBuffer;
  }

  // Overridden in active-active
  protected TransactionBuffer createTransactionBuffer(final SequenceID sid,
                                                      final TCByteBufferOutputStream newOutputStream,
                                                      final ObjectStringSerializer objectStringserializer,
                                                      final DNAEncodingInternal dnaEncoding, final TransactionID txnID) {
    return new TransactionBufferImpl(sid, newOutputStream, objectStringserializer, dnaEncoding, txnID);
  }

  private void registerKeyForOids(final Set oids, final FoldingKey key) {
    for (final Iterator i = oids.iterator(); i.hasNext();) {
      final ObjectID oid = (ObjectID) i.next();
      final Object prev = this.foldingKeys.put(oid, key);
      if (DEBUG) {
        logger.info("registered key(" + System.identityHashCode(key) + " for " + oid + ", replaces key("
                    + System.identityHashCode(prev) + ")");
      }
    }
  }

  private void log_incomingTxn(final ClientTransaction txn, final boolean exceedsLimits, final boolean scanForClose) {
    logger.info("incoming txn@" + System.identityHashCode(txn) + "[" + txn.getTransactionID() + " locks="
                + txn.getAllLockIDs() + ", oids=" + txn.getChangeBuffers().keySet() + ", dmi="
                + txn.getDmiDescriptors() + ", roots=" + txn.getNewRoots() + ", notifies=" + txn.getNotifies()
                + ", type=" + txn.getLockType() + "] exceedsLimit=" + exceedsLimits + ", scanForClose=" + scanForClose);
  }

  private void closeDependentKeys(final Collection dependentKeys) {
    for (final Iterator i = dependentKeys.iterator(); i.hasNext();) {
      final FoldingKey key = (FoldingKey) i.next();
      if (DEBUG) {
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
    final Collection locks = new HashSet(txn.getAllLockIDs()); // XXX: only create set if needed?
    final Set oids = txn.getChangeBuffers().keySet();

    for (final Iterator i = this.foldingKeys.values().iterator(); i.hasNext();) {
      final FoldingKey key = (FoldingKey) i.next();
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

  public synchronized FoldedInfo addTransaction(final ClientTransaction txn, final SequenceGenerator sequenceGenerator,
                                                final TransactionIDGenerator tidGenerator) {
    this.numTxnsBeforeFolding++;

    if (txn.getLockType().equals(TxnType.SYNC_WRITE)) {
      this.containsSyncWriteTxn = true;
    }

    removeEmptyDeltaDna(txn);

    final TransactionBuffer txnBuffer = getOrCreateBuffer(txn, sequenceGenerator, tidGenerator);

    this.bytesWritten += txnBuffer.write(txn);
    txnBuffer.addTransactionCompleteListeners(txn.getTransactionCompleteListeners());

    return new FoldedInfo(txnBuffer.getFoldedTransactionID(), txnBuffer.getTxnCount() > 1);
  }

  private void removeEmptyDeltaDna(final ClientTransaction txn) {
    for (final Iterator i = txn.getChangeBuffers().entrySet().iterator(); i.hasNext();) {
      final Map.Entry entry = (Entry) i.next();
      final TCChangeBuffer buffer = (TCChangeBuffer) entry.getValue();
      if ((!buffer.getTCObject().isNew()) && buffer.isEmpty()) {
        i.remove();
      }
    }
  }

  // Called from CommitTransactionMessageImpl
  public synchronized TCByteBuffer[] getData() {
    this.outstandingWriteCount++;
    final TCByteBufferOutputStream out = newOutputStream();
    writeHeader(out);
    for (final Iterator i = this.transactionData.values().iterator(); i.hasNext();) {
      final TransactionBuffer tb = ((TransactionBuffer) i.next());
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
  public void send() {
    final CommitTransactionMessage msg;
    synchronized (this) {
      msg = this.commitTransactionMessageFactory.newCommitTransactionMessage(this.groupID);
      msg.setBatch(this, this.serializer);
    }
    msg.send();
  }

  public synchronized Collection addTransactionIDsTo(final Collection c) {
    c.addAll(this.transactionData.keySet());
    return c;
  }

  public synchronized SequenceID getMinTransactionSequence() {
    return this.transactionData.isEmpty() ? SequenceID.NULL_ID : ((TransactionBufferImpl) this.transactionData.values()
        .iterator().next()).getSequenceID();
  }

  public Collection addTransactionSequenceIDsTo(final Collection sequenceIDs) {
    for (final Iterator i = this.transactionData.values().iterator(); i.hasNext();) {
      final TransactionBufferImpl tb = ((TransactionBufferImpl) i.next());
      sequenceIDs.add(tb.getSequenceID());
    }
    return sequenceIDs;
  }

  // Called from CommitTransactionMessageImpl recycle on write.
  public synchronized void recycle() {
    for (final Iterator iter = this.batchDataOutputStreams.iterator(); iter.hasNext();) {
      final TCByteBufferOutputStream buffer = (TCByteBufferOutputStream) iter.next();
      buffer.recycle();
    }
    this.batchDataOutputStreams.clear();
    this.outstandingWriteCount--;
  }

  public synchronized String dump() {
    final StringBuffer sb = new StringBuffer("TransactionBatchWriter = { \n");
    for (final Iterator i = this.transactionData.entrySet().iterator(); i.hasNext();) {
      final Map.Entry entry = (Entry) i.next();
      sb.append(entry.getKey()).append(" = ");
      sb.append(((TransactionBufferImpl) entry.getValue()).dump());
      sb.append("\n");
    }
    return sb.append(" } ").toString();
  }

  protected static class TransactionBufferImpl implements Recyclable, TransactionBuffer {

    private static final int               UNINITIALIZED_LENGTH = -1;

    private final SequenceID               sequenceID;
    private final TCByteBufferOutputStream output;
    private final ObjectStringSerializer   serializer;
    private final DNAEncodingInternal      encoding;
    private final Mark                     startMark;
    private final SetOnceFlag              committed            = new SetOnceFlag();

    private final Map                      writers              = new LinkedHashMap();
    private final TransactionID            txnID;

    // Maintaining hard references so that it doesn't get GC'ed on us
    private final IdentityHashMap          references           = new IdentityHashMap();
    private boolean                        needsCopy            = false;
    private int                            headerLength         = UNINITIALIZED_LENGTH;
    private int                            txnCount             = 0;
    private Mark                           changesCountMark;
    private Mark                           txnCountMark;
    private ArrayList                      txnCompleteListers;

    TransactionBufferImpl(final SequenceID sequenceID, final TCByteBufferOutputStream output,
                          final ObjectStringSerializer serializer, final DNAEncodingInternal encoding,
                          final TransactionID txnID) {
      this.sequenceID = sequenceID;
      this.output = output;
      this.serializer = serializer;
      this.encoding = encoding;
      this.startMark = output.mark();
      this.txnID = txnID;
    }

    public TransactionID getFoldedTransactionID() {
      return this.txnID;
    }

    public void writeTo(final TCByteBufferOutputStream dest) {
      // XXX: make a writeInt() and writeLong() methods on Mark. Maybe ones that take offsets too

      // This check is needed since this buffer might need to be resent upon server crash
      if (this.committed.attemptSet()) {
        this.txnCountMark.write(Conversion.int2Bytes(this.txnCount));
        this.changesCountMark.write(Conversion.int2Bytes(this.writers.size()));

        for (final Iterator i = this.writers.values().iterator(); i.hasNext();) {
          final DNAWriter writer = (DNAWriter) i.next();
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
      for (final Iterator i = this.writers.entrySet().iterator(); i.hasNext();) {
        final Map.Entry entry = (Entry) i.next();
        final DNAWriter writer = (DNAWriter) entry.getValue();

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

    public int write(final ClientTransaction txn) {
      // Holding on the object references, this method could be called more than once for folded transactions.
      // By definition on the second and subsequent calls will have repeated object references in it, so put() to the
      // map here to not store dupes.
      for (final Iterator i = txn.getReferencesOfObjectsInTxn().iterator(); i.hasNext();) {
        this.references.put(i.next(), null);
      }

      final int start = this.output.getBytesWritten();

      if (this.txnCount == 0) {
        writeFirst(txn);
      } else {
        appendChanges(txn);
      }

      this.txnCount++;

      return this.output.getBytesWritten() - start;
    }

    private void appendChanges(final ClientTransaction txn) {
      writeChanges(txn.getChangeBuffers());
    }

    private void writeChanges(final Map changes) {
      for (final Iterator i = changes.entrySet().iterator(); i.hasNext();) {
        final Map.Entry entry = (Entry) i.next();
        final ObjectID oid = (ObjectID) entry.getKey();
        final TCChangeBuffer buffer = (TCChangeBuffer) entry.getValue();

        final TCObject tco = buffer.getTCObject();

        final boolean isNew = tco.isNew();

        DNAWriter writer = (DNAWriter) this.writers.get(oid);
        if (writer == null) {
          final TCClass tcc = tco.getTCClass();
          // TODO: move LoaderDescription into DNA
          final LoaderDescription ld = tcc.getDefiningLoaderDescription();
          final String desc = ld.toDelimitedString();
          writer = new DNAWriterImpl(this.output, oid, tcc.getExtendingClassName(), this.serializer, this.encoding,
                                     desc, !isNew);

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

          if (buffer.hasMetaData()) {
            // DEV-5320
            logger.error("not sending meta data attached to \"new\" object of type " + tco.getTCClass().getName());
          }
        } else {
          buffer.writeTo(writer);
        }

        writer.markSectionEnd();

        if (!writer.isContiguous()) {
          this.needsCopy = true;
        }
      }
    }

    private void writeFirst(final ClientTransaction txn) {
      writeTransactionHeader(txn);
      final Map changes = txn.getChangeBuffers();
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

      final Collection locks = txn.getAllLockIDs();
      this.output.writeInt(locks.size());
      for (final Iterator i = locks.iterator(); i.hasNext();) {
        new LockIDSerializer((LockID) i.next()).serializeTo(this.output);
      }

      final Map newRoots = txn.getNewRoots();
      this.output.writeInt(newRoots.size());
      for (final Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
        final Entry entry = (Entry) i.next();
        final String name = (String) entry.getKey();
        final ObjectID id = (ObjectID) entry.getValue();
        this.output.writeString(name);
        this.output.writeLong(id.toLong());
      }

      final List notifies = txn.getNotifies();
      this.output.writeInt(notifies.size());
      for (final Iterator i = notifies.iterator(); i.hasNext();) {
        final Notify n = (Notify) i.next();
        n.serializeTo(this.output);
      }

      final List dmis = txn.getDmiDescriptors();
      this.output.writeInt(dmis.size());
      for (final Iterator i = dmis.iterator(); i.hasNext();) {
        final DmiDescriptor dd = (DmiDescriptor) i.next();
        dd.serializeTo(this.output);
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

    public int getTxnCount() {
      return this.txnCount;
    }

    public void recycle() {
      this.output.recycle();
    }

    public void addTransactionCompleteListeners(List transactionCompleteListeners) {
      if (!transactionCompleteListeners.isEmpty()) {
        if (txnCompleteListers == null) {
          txnCompleteListers = new ArrayList(5);
        }
        txnCompleteListers.addAll(transactionCompleteListeners);
      }
    }

    public List getTransactionCompleteListeners() {
      return (txnCompleteListers == null ? Collections.EMPTY_LIST : txnCompleteListers);
    }
  }

  private static class FoldingKey {
    private final Set               objectIDs;
    private final TxnType           txnType;
    private final TransactionBuffer buffer;
    private boolean                 closed;

    FoldingKey(final TransactionBuffer txnBuffer, final TxnType txnType, final Set objectIDs) {
      this.buffer = txnBuffer;
      this.txnType = txnType;
      this.objectIDs = objectIDs;

      if (DEBUG) {
        logger.info("created new fold key(" + System.identityHashCode(this) + "), txnType=" + txnType + ", oids="
                    + objectIDs);
      }
    }

    Set getObjectIDs() {
      return this.objectIDs;
    }

    public void close() {
      this.closed = true;
    }

    public boolean isClosed() {
      return this.closed;
    }

    public boolean hasCommonality(final Collection locks, final Collection oids) {
      return CollectionUtils.containsAny(this.objectIDs, oids);
    }

    public TransactionBuffer getBuffer() {
      return this.buffer;
    }

    public boolean canAcceptFold(final List txnLocks, final TxnType type) {
      // relax folding rules to allow folding txn with common object even lock sets different
      if (!type.equals(this.txnType)) {
        if (DEBUG) {
          logger.info(System.identityHashCode(this) + ": not accepting fold since txn type is different");
        }
        return false;
      }

      if (DEBUG) {
        logger.info(System.identityHashCode(this) + ": fold accepted");
      }

      return true;
    }
  }

  public static class FoldingConfig {
    private final int     lockLimit;
    private final int     objectLimit;
    private final boolean foldingEnabled;

    public FoldingConfig(final boolean foldingEnabled, final int objectLimit, final int lockLimit) {
      this.foldingEnabled = foldingEnabled;
      this.objectLimit = objectLimit;
      this.lockLimit = lockLimit;
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

    public static FoldingConfig createFromProperties(final TCProperties props) {
      return new FoldingConfig(props.getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_ENABLED),
                               props.getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT),
                               props.getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT));
    }
  }

  public static class FoldedInfo {
    private final TransactionID txnID;
    private final boolean       folded;

    public FoldedInfo(final TransactionID txnID, final boolean folded) {
      this.txnID = txnID;
      this.folded = folded;
    }

    public TransactionID getFoldedTransactionID() {
      return this.txnID;
    }

    public boolean isFolded() {
      return this.folded;
    }
  }

}
