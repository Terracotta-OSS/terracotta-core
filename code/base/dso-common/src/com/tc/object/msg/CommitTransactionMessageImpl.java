/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.lang.Recyclable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionBatch;
import com.tc.object.tx.TransactionID;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author steve
 */
public class CommitTransactionMessageImpl extends DSOMessageBase implements EventContext, CommitTransactionMessage {
  private static final byte      BATCH_TRANSACTION_DATA_ID   = 1;
  private static final byte      ACKNOWLEDGED_TRANSACTION_ID = 2;
  private static final byte      SERIALIZER_ID               = 3;
  private ObjectStringSerializer serializer;

  private Recyclable             batch;                                      // This is used to recycle buffers on
  // the write side
  private TCByteBuffer[]         batchData;
  private final Set              acknowledgedTransactionIDs  = new HashSet();

  public CommitTransactionMessageImpl(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                      TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public CommitTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,
                                      TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  protected void dehydrateValues() {
    for (Iterator i = acknowledgedTransactionIDs.iterator(); i.hasNext();) {
      putNVPair(ACKNOWLEDGED_TRANSACTION_ID, ((TransactionID) i.next()).toLong());
    }
    putNVPair(SERIALIZER_ID, serializer);
    putNVPair(BATCH_TRANSACTION_DATA_ID, batchData);
    batchData = null;
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_TRANSACTION_DATA_ID: {
        this.batchData = getInputStream().toArray();
        return true;
      }
      case ACKNOWLEDGED_TRANSACTION_ID: {
        this.acknowledgedTransactionIDs.add(new TransactionID(getInputStream().readLong()));
        return true;
      }
      case SERIALIZER_ID:
        this.serializer = (ObjectStringSerializer) getObject(new ObjectStringSerializer());
        return true;

      default: {
        return false;
      }
    }
  }

  public void setBatch(TransactionBatch batch, ObjectStringSerializer serializer) {
    this.batch = batch;
    setBatchData(batch.getData(), serializer);
    this.acknowledgedTransactionIDs.addAll(batch.getAcknowledgedTransactionIDs());
  }

  // This is here for a test
  synchronized void setBatchData(TCByteBuffer[] batchData, ObjectStringSerializer serializer) {
    if (this.batchData != null) throw new AssertionError("Attempt to set TransactionBatch more than once.");
    this.batchData = batchData;
    this.serializer = serializer;
  }

  public synchronized TCByteBuffer[] getBatchData() {
    return batchData;
  }

  public Collection getAcknowledgedTransactionIDs() {
    return Collections.unmodifiableCollection(this.acknowledgedTransactionIDs);
  }

  public void doRecycleOnRead() {
    // recycle will be called later
  }

  protected boolean isOutputStreamRecycled() {
    return true;
  }

  public void doRecycleOnWrite() {
    // recycle only those buffers created for this message
    recycleOutputStream();
    if (batch != null) {
      batch.recycle();
    }
  }

}
