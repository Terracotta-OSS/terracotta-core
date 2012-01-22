/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionBatch;

import java.io.IOException;

/**
 * @author steve
 */
public class CommitTransactionMessageImpl extends DSOMessageBase implements EventContext, CommitTransactionMessage {
  private static final byte      BATCH_TRANSACTION_DATA_ID = 1;
  private static final byte      SERIALIZER_ID             = 2;

  // This is used to recycle buffers on the write side
  private Recyclable             batch;
  private TCByteBuffer[]         batchData;
  private ObjectStringSerializer serializer;

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

  @Override
  protected void dehydrateValues() {
    putNVPair(SERIALIZER_ID, serializer);
    putNVPair(BATCH_TRANSACTION_DATA_ID, batchData);
    batchData = null;
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_TRANSACTION_DATA_ID: {
        this.batchData = getInputStream().toArray();
        return true;
      }
      case SERIALIZER_ID:
        this.serializer = (ObjectStringSerializer) getObject(new ObjectStringSerializerImpl());
        return true;

      default: {
        return false;
      }
    }
  }

  public void setBatch(TransactionBatch batch, ObjectStringSerializer serializer) {
    this.batch = batch;
    if (this.batchData != null) throw new AssertionError("Attempt to set TransactionBatch more than once.");
    this.batchData = batch.getData();
    this.serializer = serializer;
  }

  public TCByteBuffer[] getBatchData() {
    return batchData;
  }

  @Override
  public void doRecycleOnRead() {
    // recycle will be called later
  }

  @Override
  protected boolean isOutputStreamRecycled() {
    return true;
  }

  @Override
  public void doRecycleOnWrite() {
    // recycle only those buffers created for this message
    recycleOutputStream();
    if (batch != null) {
      batch.recycle();
    }
  }
}
