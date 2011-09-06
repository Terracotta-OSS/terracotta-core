/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAWriterInternal;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIDSerializer;
import com.tc.object.locks.Notify;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This transaction Batch writer is used at the server side to create transactions
 */
public class ServerTransactionBatchWriter {

  private static final DNAEncodingInternal DNA_STORAGE_ENCODING = new StorageDNAEncodingImpl();

  private final ObjectStringSerializer     serializer;
  private final TxnBatchID                 batchId;

  public ServerTransactionBatchWriter(final TxnBatchID batchId, final ObjectStringSerializer serializer) {
    this.batchId = batchId;
    this.serializer = serializer;
  }

  public TCByteBuffer[] writeTransactionBatch(final List<ServerTransaction> txns) throws IOException,
      ClassNotFoundException {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream(32, 4096, false);
    out.writeLong(this.batchId.toLong());
    out.writeInt(txns.size());
    out.writeBoolean(containsSyncWriteTransaction(txns));
    for (final ServerTransaction serverTransaction : txns) {
      writeTransaction(out, serverTransaction);
    }
    return out.toArray();
  }

  private void writeTransaction(final TCByteBufferOutputStream out, final ServerTransaction txn) throws IOException,
      ClassNotFoundException {
    out.writeLong(txn.getTransactionID().toLong());
    out.writeByte(txn.getTransactionType().getType());
    out.writeInt(txn.getNumApplicationTxn());
    out.writeLong(txn.getClientSequenceID().toLong());

    writeLockIDs(out, txn.getLockIDs());
    writeRootsMap(out, txn.getNewRoots());
    writeNotifies(out, txn.getNotifies());
    writeDMIDescriptors(out, txn.getDmiDescriptors());
    writeHighWaterMarks(out, txn.getHighWaterMarks());
    writeDNAs(out, txn.getChanges());
  }

  private void writeDNAs(final TCByteBufferOutputStream out, final List changes) throws IOException,
      ClassNotFoundException {
    out.writeInt(changes.size());
    for (final Iterator i = changes.iterator(); i.hasNext();) {
      final DNAInternal dna = (DNAInternal) i.next();
      writeDNA(out, dna);
    }
  }

  private void writeDNA(final TCByteBufferOutputStream out, final DNAInternal dna) throws IOException,
      ClassNotFoundException {
    final DNAWriterInternal dnaWriter = new DNAWriterImpl(out, dna.getObjectID(), dna.getTypeName(), this.serializer,
                                                          DNA_STORAGE_ENCODING, dna.getDefiningLoaderDescription(),
                                                          dna.isDelta());
    writeParentObjectID(dnaWriter, dna.getParentObjectID());
    // It is assumed that if this DNA is shared/accessed by multiple threads (simultaneously or other wise) that the DNA
    // is thread safe and the DNA gives out multiple iteratable cursors or the cursor is resettable
    final DNACursor cursor = dna.getCursor();
    addActions(dnaWriter, cursor, DNA_STORAGE_ENCODING, dna);
    for (MetaDataDescriptorInternal mdd : dna.getMetaDataReader()) {
      dnaWriter.addMetaData(mdd);
    }
    dnaWriter.markSectionEnd();
    dnaWriter.finalizeHeader();
  }

  // TODO:: Move these "addAction" methods into DNAWrite (when tim-api is allowed to change) so it resides closer to
  // other related logics.
  private void addActions(final DNAWriter dnaWriter, final DNACursor cursor, final DNAEncoding decoder, final DNA dna)
      throws IOException, ClassNotFoundException {
    while (cursor.next(decoder)) {
      final Object action = cursor.getAction();
      if (action instanceof PhysicalAction) {
        writePhysicalAction(dnaWriter, (PhysicalAction) action);
      } else if (action instanceof LogicalAction) {
        writeLogicalAction(dnaWriter, (LogicalAction) action);
      } else if (action instanceof LiteralAction) {
        writeLiteralAction(dnaWriter, (LiteralAction) action);
      } else {
        throw new AssertionError("Unknown action type : " + action + " in dna : " + dna);
      }
    }
  }

  private void writeParentObjectID(final DNAWriter dnaWriter, final ObjectID parentObjectID) {
    if (!parentObjectID.isNull()) {
      dnaWriter.setParentObjectID(parentObjectID);
    }
  }

  private void writeLiteralAction(final DNAWriter dnaWriter, final LiteralAction action) {
    dnaWriter.addLiteralValue(action.getObject());
  }

  private void writeLogicalAction(final DNAWriter dnaWriter, final LogicalAction action) {
    dnaWriter.addLogicalAction(action.getMethod(), action.getParameters());
  }

  private void writePhysicalAction(final DNAWriter dnaWriter, final PhysicalAction action) {
    if (action.isTruePhysical()) {
      dnaWriter.addPhysicalAction(action.getFieldName(), action.getObject(), action.isReference());
    } else if (action.isArrayElement()) {
      dnaWriter.addArrayElementAction(action.getArrayIndex(), action.getObject());
    } else if (action.isEntireArray()) {
      dnaWriter.setArrayLength(Array.getLength(action.getObject()));
      dnaWriter.addEntireArray(action.getObject());
    } else if (action.isSubArray()) {
      dnaWriter.addSubArrayAction(action.getArrayIndex(), action.getObject(), Array.getLength(action.getObject()));
    } else {
      throw new AssertionError("Unknown Physical Action : " + action);
    }

  }

  private void writeHighWaterMarks(final TCByteBufferOutputStream out, final long[] highWaterMarks) {
    out.writeInt(highWaterMarks.length);
    for (final long h : highWaterMarks) {
      out.writeLong(h);
    }
  }

  private void writeDMIDescriptors(final TCByteBufferOutputStream out, final DmiDescriptor[] dmiDescriptors) {
    out.writeInt(dmiDescriptors.length);
    for (final DmiDescriptor dmiDescriptor : dmiDescriptors) {
      dmiDescriptor.serializeTo(out);
    }
  }

  private void writeNotifies(final TCByteBufferOutputStream out, final Collection notifies) {
    out.writeInt(notifies.size());
    for (final Iterator i = notifies.iterator(); i.hasNext();) {
      final Notify n = (Notify) i.next();
      n.serializeTo(out);
    }
  }

  private void writeRootsMap(final TCByteBufferOutputStream out, final Map newRoots) {
    out.writeInt(newRoots.size());
    for (final Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
      final Entry<String, ObjectID> e = (Entry<String, ObjectID>) i.next();
      out.writeString(e.getKey());
      out.writeLong(e.getValue().toLong());
    }
  }

  private void writeLockIDs(final TCByteBufferOutputStream out, final LockID[] lockIDs) {
    out.writeInt(lockIDs.length);
    for (final LockID lockID : lockIDs) {
      final LockIDSerializer lidsr = new LockIDSerializer(lockID);
      lidsr.serializeTo(out);
    }
  }

  private boolean containsSyncWriteTransaction(final List<ServerTransaction> txns) {
    for (final ServerTransaction serverTransaction : txns) {
      if (serverTransaction.getTransactionType() == TxnType.SYNC_WRITE) { return true; }
    }
    return false;
  }

}
