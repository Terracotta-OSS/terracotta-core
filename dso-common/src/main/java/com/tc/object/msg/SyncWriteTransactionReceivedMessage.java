/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SyncWriteTransactionReceivedMessage extends DSOMessageBase {
  private final static byte BATCH_ID = 1;
  private final static byte TXN_SET  = 2;

  private long              batchID;
  private SyncSetSerializer syncTxnSet;

  public SyncWriteTransactionReceivedMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                             MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public SyncWriteTransactionReceivedMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                             TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(BATCH_ID, batchID);
    putNVPair(TXN_SET, syncTxnSet);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case BATCH_ID:
        batchID = getLongValue();
        return true;
      case TXN_SET:
        syncTxnSet = (SyncSetSerializer) getObject(new SyncSetSerializer());
        return true;
      default:
        return false;
    }
  }

  public void initialize(long batchId, Set<TransactionID> syncSet) {
    this.batchID = batchId;
    this.syncTxnSet = new SyncSetSerializer(syncSet);
  }

  public long getBatchID() {
    return batchID;
  }

  public Set<TransactionID> getSyncTxnSet() {
    return syncTxnSet.getSet();
  }

  private static class SyncSetSerializer implements TCSerializable {
    private Set<TransactionID> syncTxnSet;

    public SyncSetSerializer() {
      //
    }

    public SyncSetSerializer(Set<TransactionID> syncTxnSet) {
      this.syncTxnSet = syncTxnSet;
    }

    public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
      syncTxnSet = new HashSet<TransactionID>();
      int size = serialInput.readInt();
      for (int i = 0; i < size; i++) {
        syncTxnSet.add(new TransactionID(serialInput.readLong()));
      }
      return this;
    }

    public void serializeTo(TCByteBufferOutput serialOutput) {
      serialOutput.writeInt(syncTxnSet.size());
      for (TransactionID txId : syncTxnSet) {
        serialOutput.writeLong(txId.toLong());
      }
    }

    public Set<TransactionID> getSet() {
      return syncTxnSet;
    }

  }
}
