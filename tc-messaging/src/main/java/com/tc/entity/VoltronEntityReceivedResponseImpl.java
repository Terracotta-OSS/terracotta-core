/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

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


public class VoltronEntityReceivedResponseImpl extends DSOMessageBase implements VoltronEntityReceivedResponse {
  private static final byte TRANSACTION_ID = 0;
  
  private TransactionID transactionID;
  
  public VoltronEntityReceivedResponseImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public VoltronEntityReceivedResponseImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public VoltronEntityMessage.Acks getAckType() {
    return VoltronEntityMessage.Acks.RECEIVED;
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
