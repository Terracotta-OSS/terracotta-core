/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.lang.Recyclable;
import com.tc.object.ObjectID;
import com.tc.object.change.TCChangeBuffer;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.Notify;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class TransactionBatchWriter implements ClientTransactionBatch {

  private final CommitTransactionMessageFactory commitTransactionMessageFactory;
  private final Set                             acknowledgedTransactionIDs = new HashSet();
  private final TxnBatchID                      batchID;
  private final LinkedHashMap                   transactionData            = new LinkedHashMap();
  private final ObjectStringSerializer          serializer;
  private final DNAEncoding                     encoding;
  private int                                   txns2Serialize             = 0;
  private final List                            batchDataOutputStreams     = new ArrayList();
  private short                                 outstandingWriteCount      = 0;
  private int                                   bytesWritten               = 0;

  public TransactionBatchWriter(TxnBatchID batchID, ObjectStringSerializer serializer, DNAEncoding encoding,
                                CommitTransactionMessageFactory commitTransactionMessageFactory) {
    this.batchID = batchID;
    this.encoding = encoding;
    this.commitTransactionMessageFactory = commitTransactionMessageFactory;
    this.serializer = serializer;
  }

  public String toString() {
    return super.toString() + "[" + this.batchID + ", isEmpty=" + isEmpty() + ", size=" + numberOfTxns()
           + ", txns2Serialize =" + txns2Serialize + "]";
  }

  public TxnBatchID getTransactionBatchID() {
    return this.batchID;
  }

  public synchronized boolean isEmpty() {
    return transactionData.isEmpty();
  }

  public synchronized int numberOfTxns() {
    return transactionData.size();
  }

  public synchronized int byteSize() {
    return bytesWritten;
  }

  public boolean isNull() {
    return false;
  }

  public synchronized void removeTransaction(TransactionID txID) {
    TransactionDescriptor removed = (TransactionDescriptor) transactionData.remove(txID);
    if (removed == null) throw new AssertionError("Attempt to remove a transaction that doesn't exist : " + removed);
    // if we get some acks from the previous instance of the server after we resend this
    // transaction, but before we write to the network, then we dont recycle. We lose those
    // buffers. But since it is a rare scenario we dont lose much, but this check avoid writting
    // corrupt buffers.
    if (outstandingWriteCount == 0) removed.recycle();
  }

  public synchronized void addTransaction(ClientTransaction txn) {
    SequenceID sequenceID = txn.getSequenceID();
    TCByteBufferOutputStream out = newOutputStream();

    // /////////////////////////////////////////////////////////////////////////////////////////
    // If you're modifying this format, you'll need to update
    // TransactionBatchReader as well //
    // /////////////////////////////////////////////////////////////////////////////////////////

    out.writeLong(txn.getTransactionID().toLong());
    out.writeByte(txn.getTransactionType().getType());
    SequenceID sid = txn.getSequenceID();
    if (sid.isNull()) throw new AssertionError("SequenceID is null: " + txn);
    out.writeLong(sid.toLong());

    LockID[] locks = txn.getAllLockIDs();
    out.writeInt(locks.length);
    for (int i = 0, n = locks.length; i < n; i++) {
      out.writeString(locks[i].asString());
    }

    Map newRoots = txn.getNewRoots();
    out.writeInt(newRoots.size());
    for (Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      String name = (String) entry.getKey();
      ObjectID id = (ObjectID) entry.getValue();
      out.writeString(name);
      out.writeLong(id.toLong());
    }

    List notifies = txn.addNotifiesTo(new LinkedList());
    out.writeInt(notifies.size());
    for (Iterator i = notifies.iterator(); i.hasNext();) {
      Notify n = (Notify) i.next();
      n.serializeTo(out);
    }

    List dmis = txn.getDmiDescriptors();
    out.writeInt(dmis.size());
    for (Iterator i = dmis.iterator(); i.hasNext();) {
      DmiDescriptor dd = (DmiDescriptor) i.next();
      dd.serializeTo(out);
    }

    Map changes = txn.getChangeBuffers();
    out.writeInt(changes.size());
    for (Iterator i = changes.values().iterator(); i.hasNext();) {
      TCChangeBuffer buffer = (TCChangeBuffer) i.next();
      buffer.writeTo(out, serializer, encoding);
    }
    
    bytesWritten += out.getBytesWritten();
    transactionData.put(txn.getTransactionID(), new TransactionDescriptor(sequenceID, out.toArray(), txn
        .getReferencesOfObjectsInTxn()));
  }

  // Called from CommitTransactionMessageImpl
  public synchronized TCByteBuffer[] getData() {
    outstandingWriteCount++;
    TCByteBufferOutputStream out = newOutputStream();
    writeHeader(out);
    for (Iterator i = transactionData.values().iterator(); i.hasNext();) {
      TransactionDescriptor td = ((TransactionDescriptor) i.next());
      TCByteBuffer[] data = td.getData();
      out.write(data);
    }
    batchDataOutputStreams.add(out);

    // System.err.println("Batch size: " + out.getBytesWritten() + ", # TXNs = " + numberOfTxns());

    return out.toArray();
  }

  private void writeHeader(TCByteBufferOutputStream out) {
    out.writeLong(this.batchID.toLong());
    out.writeInt(numberOfTxns());
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

  public synchronized void addAcknowledgedTransactionIDs(Collection acknowledged) {
    this.acknowledgedTransactionIDs.addAll(acknowledged);
  }

  public Collection getAcknowledgedTransactionIDs() {
    return this.acknowledgedTransactionIDs;
  }

  public synchronized SequenceID getMinTransactionSequence() {
    return transactionData.isEmpty() ? SequenceID.NULL_ID : ((TransactionDescriptor) transactionData.values()
        .iterator().next()).getSequenceID();
  }

  public Collection addTransactionSequenceIDsTo(Collection sequenceIDs) {
    for (Iterator i = transactionData.values().iterator(); i.hasNext();) {
      TransactionDescriptor td = ((TransactionDescriptor) i.next());
      sequenceIDs.add(td.getSequenceID());
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
      sb.append(((TransactionDescriptor) entry.getValue()).dump());
      sb.append("\n");
    }
    return sb.append(" } ").toString();
  }

  /**
   * This is for testing only.
   */
  public synchronized void wait4AllTxns2Serialize() {
    while (txns2Serialize != 0) {
      try {
        wait(2000);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  private static final class TransactionDescriptor implements Recyclable {

    final SequenceID         sequenceID;
    final TCByteBuffer[]     data;
    // Maintaining hard references so that it doesnt get GCed on us
    private final Collection references;

    TransactionDescriptor(SequenceID sequenceID, TCByteBuffer[] data, Collection references) {
      this.sequenceID = sequenceID;
      this.data = data;
      this.references = references;
    }

    public String dump() {
      return " { " + sequenceID + " , Objects in Txn = " + references.size() + " }";
    }

    SequenceID getSequenceID() {
      return this.sequenceID;
    }

    TCByteBuffer[] getData() {
      return data;
    }

    public void recycle() {
      for (int i = 0; i < data.length; i++) {
        data[i].recycle();
      }
    }
  }

}
