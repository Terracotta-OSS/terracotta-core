package com.tc.entity;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;

import java.io.IOException;


/**
 * This implementation of VoltronEntityMessage is purely to handle the case of transaction resends when the message data
 * is required, but it is sent as part of a larger message, not as a stand-alone message.
 */
public class ResendVoltronEntityMessage implements VoltronEntityMessage, TCSerializable<ResendVoltronEntityMessage> {
  private NodeID source;
  private TransactionID transactionID;
  private EntityDescriptor entityDescriptor;
  private Type type;
  private boolean requiresReplication;
  private byte[] extendedData;
  private TransactionID oldestTransactionPending;

  public ResendVoltronEntityMessage() {
    // to make TCSerializable happy
  }

  public ResendVoltronEntityMessage(NodeID source, TransactionID transactionID, EntityDescriptor entityDescriptor, Type type, boolean requiresReplication, byte[] extendedData, TransactionID oldestTransactionPending) {
    this.source = source;
    this.transactionID = transactionID;
    this.entityDescriptor = entityDescriptor;
    this.type = type;
    this.requiresReplication = requiresReplication;
    this.extendedData = extendedData;
    this.oldestTransactionPending = oldestTransactionPending;
  }

  @Override
  public NodeID getSource() {
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
  public Type getType() {
    Assert.assertNotNull(this.type);
    return this.type;
  }
  
  @Override
  public byte[] getExtendedData() {
    Assert.assertNotNull(this.extendedData);
    return this.extendedData;
  }

  @Override
  public TransactionID getOldestTransactionOnClient() {
    return this.oldestTransactionPending;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    this.source.serializeTo(serialOutput);
    serialOutput.writeLong(this.transactionID.toLong());
    this.entityDescriptor.serializeTo(serialOutput);
    serialOutput.writeInt(this.type.ordinal());
    serialOutput.writeBoolean(this.requiresReplication);
    serialOutput.writeInt(extendedData.length);
    serialOutput.write(extendedData);
    serialOutput.writeLong(this.oldestTransactionPending.toLong());
  }

  @Override
  public ResendVoltronEntityMessage deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.source = ClientID.readFrom(serialInput);
    this.transactionID = new TransactionID(serialInput.readLong());
    this.entityDescriptor = EntityDescriptor.readFrom(serialInput);
    this.type = Type.values()[serialInput.readInt()];
    this.requiresReplication = serialInput.readBoolean();
    int bufferLength = serialInput.readInt();
    this.extendedData = new byte[bufferLength];
    serialInput.read(this.extendedData);
    this.oldestTransactionPending = new TransactionID(serialInput.readLong());
    return this;
  }
}
