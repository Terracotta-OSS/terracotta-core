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
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityDescriptor;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;

import org.terracotta.entity.EntityMessage;


public class DiagnosticMessageImpl extends DSOMessageBase implements DiagnosticMessage {
  private TransactionID transactionID;
  private byte[] extendedData;

  @Override
  public ClientID getSource() {
    return ClientID.NULL_ID;
  }
  @Override
  public TransactionID getTransactionID() {
    return transactionID;
  }
  
  @Override
  public EntityDescriptor getEntityDescriptor() {
    return EntityDescriptor.NULL_ID;
  }

  @Override
  public boolean doesRequireReplication() {
    return false;
  }
  
  @Override
  public Type getVoltronType() {
    return Type.NOOP;
  }
  
  @Override
  public byte[] getExtendedData() {
    return this.extendedData;
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    return TransactionID.NULL_ID;
  }

  @Override
  public void setContents(TransactionID transactionID, byte[] extendedData) {
    this.transactionID = transactionID;
    this.extendedData = extendedData;
  }

  public DiagnosticMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public DiagnosticMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair((byte)0, (byte)0);
        
    outputStream.writeLong(this.transactionID.toLong());
    
    outputStream.writeInt(extendedData.length);
    outputStream.write(extendedData);
  }
  
  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    Assert.assertTrue(0 == name);
    // Read our dummy byte.
    getByteValue();
    
    this.transactionID = new TransactionID(getLongValue());
    this.extendedData = getBytesArray();
    
    return true;
  }

  @Override
  public EntityMessage getEntityMessage() {
    return null;
  }

}
