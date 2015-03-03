package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;

/**
 * @author twu
 */
public class RequestAckMessageImpl extends DSOMessageBase implements RequestAckMessage {
  private static final byte TRANSACTION_ID = 0;
  
  private TransactionID transactionID;
  
  public RequestAckMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public RequestAckMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(TRANSACTION_ID, transactionID.toLong());
  }

  @Override
  public void setTransactionID(TransactionID id) {
    transactionID = id;
  }

  @Override
  public TransactionID getTransactionID() {
    return transactionID;
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (name != TRANSACTION_ID) return false;
    transactionID = new TransactionID(getLongValue());
    return true;
  }
}
