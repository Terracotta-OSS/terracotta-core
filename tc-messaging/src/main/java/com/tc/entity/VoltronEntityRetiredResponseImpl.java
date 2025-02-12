/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.entity;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;


public class VoltronEntityRetiredResponseImpl extends DSOMessageBase implements VoltronEntityRetiredResponse {
  private static final byte TRANSACTION_ID = 0;
  
  private TransactionID transactionID;
  
  public VoltronEntityRetiredResponseImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public VoltronEntityRetiredResponseImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public VoltronEntityMessage.Acks getAckType() {
    return VoltronEntityMessage.Acks.RETIRED;
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
    boolean didProcess = false;
    if (TRANSACTION_ID == name) {
      this.transactionID = new TransactionID(getLongValue());
      didProcess = true;
    }
    return didProcess;
  }
}
