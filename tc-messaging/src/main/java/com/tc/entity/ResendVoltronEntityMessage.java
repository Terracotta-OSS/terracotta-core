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

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;

import org.terracotta.entity.EntityMessage;


/**
 * This implementation of VoltronEntityMessage is purely to handle the case of transaction resends when the message data
 * is required, but it is sent as part of a larger message, not as a stand-alone message.
 */
public class ResendVoltronEntityMessage implements VoltronEntityMessage, TCSerializable<ResendVoltronEntityMessage> {
  private ClientID source;
  private TransactionID transactionID;
  private EntityDescriptor entityDescriptor;
  private Type type;
  private boolean requiresReplication;
  private TCByteBuffer extendedData;

  public ResendVoltronEntityMessage() {
    // to make TCSerializable happy
  }

  public ResendVoltronEntityMessage(ClientID source, TransactionID transactionID, EntityDescriptor entityDescriptor, Type type, boolean requiresReplication, TCByteBuffer extendedData) {
    this.source = source;
    this.transactionID = transactionID;
    this.entityDescriptor = entityDescriptor;
    this.type = type;
    this.requiresReplication = requiresReplication;
    this.extendedData = extendedData == null || extendedData.isReadOnly() ? extendedData : extendedData.asReadOnlyBuffer();
  }

  @Override
  public ClientID getSource() {
    Assert.assertNotNull(this.source);
    return this.source;
  }
  @Override
  public TransactionID getTransactionID() {
    Assert.assertNotNull(this.transactionID);
    return this.transactionID;
  }
  
  @Override
  public EntityDescriptor getEntityDescriptor() {
    Assert.assertNotNull(this.entityDescriptor);
    return this.entityDescriptor;
  }

  @Override
  public boolean doesRequireReplication() {
    return this.requiresReplication;
  }

  @Override
  public boolean doesRequestReceived() {
// if it's a resend, this already happened or it didn't
    return false;
  }

  @Override
  public boolean doesRequestRetired() {
// if it's a resend, this already happened or it didn't
    return false;
  }
  
  @Override
  public Type getVoltronType() {
    Assert.assertNotNull(this.type);
    return this.type;
  }
  
  @Override
  public TCByteBuffer getExtendedData() {
    Assert.assertNotNull(this.extendedData);
    return this.extendedData.duplicate();
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    return TransactionID.FIRST_ID;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    this.source.serializeTo(serialOutput);
    serialOutput.writeLong(this.transactionID.toLong());
    this.entityDescriptor.serializeTo(serialOutput);
    serialOutput.writeInt(this.type.ordinal());
    serialOutput.writeBoolean(this.requiresReplication);
    serialOutput.writeInt(extendedData.remaining());
    serialOutput.write(extendedData.duplicate());
    serialOutput.writeLong(TransactionID.FIRST_ID.toLong());
  }

  @Override
  public ResendVoltronEntityMessage deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.source = ClientID.readFrom(serialInput);
    this.transactionID = new TransactionID(serialInput.readLong());
    this.entityDescriptor = EntityDescriptor.readFrom(serialInput);
    this.type = Type.values()[serialInput.readInt()];
    this.requiresReplication = serialInput.readBoolean();
    int bufferLength = serialInput.readInt();
    this.extendedData = serialInput.read(bufferLength);
    long compatRead = serialInput.readLong();
    return this;
  }

  @Override
  public EntityMessage getEntityMessage() {
    // There is no built-in message.
    return null;
  }

  @Override
  public String toString() {
    return "ResendVoltronEntityMessage{" + "source=" + source + ", transactionID=" + transactionID + ", entityDescriptor=" + entityDescriptor + '}';
  }
}
