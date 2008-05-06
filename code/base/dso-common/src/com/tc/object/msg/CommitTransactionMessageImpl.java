/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.EventContext;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.lang.Recyclable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionBatch;

import java.io.IOException;

/**
 * @author steve
 */
public class CommitTransactionMessageImpl extends DSOMessageBase implements EventContext, CommitTransactionMessage {
  private static final byte      BATCH_TRANSACTION_DATA_ID = 1;
  private static final byte      SERIALIZER_ID             = 2;
  private ObjectStringSerializer serializer;

  private Recyclable             batch;                        // This is used to recycle buffers on
  // the write side
  private TCByteBuffer[]         batchData;

  public CommitTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                      MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public CommitTransactionMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                      TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  protected void dehydrateValues() {
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
