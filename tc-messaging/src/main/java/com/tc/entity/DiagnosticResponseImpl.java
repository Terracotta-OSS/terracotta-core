/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import com.tc.util.Assert;

import java.io.IOException;


public class DiagnosticResponseImpl extends DSOMessageBase implements DiagnosticResponse {
  private TransactionID transactionID;
  private byte[] successResponse;
  
  @Override
  public void setResponse(TransactionID transactionID, byte[] response) {
    this.transactionID = transactionID;
    this.successResponse = response;
  }

  @Override
  public VoltronEntityMessage.Acks getAckType() {
    return VoltronEntityMessage.Acks.COMPLETED;
  }
  
  public DiagnosticResponseImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public DiagnosticResponseImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair((byte)0, (byte)0);
    
    outputStream.writeLong(this.transactionID.toLong());

    outputStream.writeInt(this.successResponse.length);
    outputStream.write(this.successResponse);
  }
  
  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    Assert.assertTrue(0 == name);
    Assert.assertTrue(null == this.transactionID);
    // Read our dummy byte.
    getByteValue();
    
    this.transactionID = new TransactionID(getLongValue());
    this.successResponse = getBytesArray();
    return true;
  }

  @Override
  public TransactionID getTransactionID() {
    return this.transactionID;
  }

  @Override
  public byte[] getResponse() {
    return this.successResponse;
  }
}
