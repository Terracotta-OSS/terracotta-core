/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LiteralAction;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIDSerializer;
import com.tc.object.locks.Notify;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
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

  public ServerTransactionBatchWriter(TxnBatchID batchId, ObjectStringSerializer serializer) {
    this.batchId = batchId;
    this.serializer = serializer;
  }


  @SuppressWarnings("resource")
  public TCByteBuffer[] writeTransactionBatch(List<ServerTransaction> txns) throws IOException,
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

  private void writeTransaction(TCByteBufferOutputStream out, ServerTransaction txn) throws IOException,
      ClassNotFoundException {
    out.writeLong(txn.getTransactionID().toLong());
    out.writeByte(txn.getTransactionType().getType());
    out.writeInt(txn.getTxnCount());
    out.writeLong(txn.getClientSequenceID().toLong());
    out.writeBoolean(txn.isEviction());

    writeLockIDs(out, txn.getLockIDs());
    writeNotifies(out, txn.getNotifies());
    writeDNAs(out, txn.getChanges());
  }

  private void writeDNAs(TCByteBufferOutputStream out, List<? extends DNA> changes) throws IOException,
      ClassNotFoundException {
    out.writeInt(changes.size());
    for (DNA dna : changes) {
      writeDNA(out, dna);
    }
  }

  private void writeDNA(TCByteBufferOutputStream out, DNA dna) throws IOException,
      ClassNotFoundException {
    final DNAWriter dnaWriter = new DNAWriterImpl(out, dna.getEntityID(), this.serializer, DNA_STORAGE_ENCODING, dna.isDelta());
    // It is assumed that if this DNA is shared/accessed by multiple threads (simultaneously or other wise) that the DNA
    // is thread safe and the DNA gives out multiple iteratable cursors or the cursor is resettable
    final DNACursor cursor = dna.getCursor();
    addActions(dnaWriter, cursor, DNA_STORAGE_ENCODING, dna);
    dnaWriter.markSectionEnd();
    dnaWriter.finalizeHeader();
  }

  // TODO:: Move these "addAction" methods into DNAWrite (when tim-api is allowed to change) so it resides closer to
  // other related logics.
  private void addActions(DNAWriter dnaWriter, DNACursor cursor, DNAEncoding decoder, DNA dna)
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

  private void writeLiteralAction(DNAWriter dnaWriter, LiteralAction action) {
    dnaWriter.addLiteralValue(action.getObject());
  }

  private void writeLogicalAction(DNAWriter dnaWriter, LogicalAction action) {
    dnaWriter.addLogicalAction(action.getLogicalOperation(), action.getParameters());
  }

  private void writePhysicalAction(DNAWriter dnaWriter, PhysicalAction action) {
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

  private void writeHighWaterMarks(TCByteBufferOutputStream out, long[] highWaterMarks) {
    out.writeInt(highWaterMarks.length);
    for (final long h : highWaterMarks) {
      out.writeLong(h);
    }
  }

  private void writeNotifies(TCByteBufferOutputStream out, Collection<Notify> notifies) {
    out.writeInt(notifies.size());
    for (final Notify n : notifies) {
      n.serializeTo(out);
    }
  }

  private void writeRootsMap(TCByteBufferOutputStream out, Map<String, ObjectID> newRoots) {
    out.writeInt(newRoots.size());
    for (Entry<String, ObjectID> e : newRoots.entrySet()) {
      out.writeString(e.getKey());
      out.writeLong(e.getValue().toLong());
    }
  }

  private void writeLockIDs(TCByteBufferOutputStream out, LockID[] lockIDs) {
    out.writeInt(lockIDs.length);
    for (final LockID lockID : lockIDs) {
      final LockIDSerializer lidsr = new LockIDSerializer(lockID);
      lidsr.serializeTo(out);
    }
  }

  private boolean containsSyncWriteTransaction(List<ServerTransaction> txns) {
    for (final ServerTransaction serverTransaction : txns) {
      if (serverTransaction.getTransactionType() == TxnType.SYNC_WRITE) { return true; }
    }
    return false;
  }

}
