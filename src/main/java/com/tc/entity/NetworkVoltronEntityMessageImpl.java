package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
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


public class NetworkVoltronEntityMessageImpl extends DSOMessageBase implements NetworkVoltronEntityMessage {
  private ClientID clientID;
  private TransactionID transactionID;
  private EntityDescriptor entityDescriptor;
  private Type type;
  private boolean requiresReplication;
  private byte[] extendedData;

  @Override
  public NodeID getSource() {
    Assert.assertNotNull(this.clientID);
    return this.clientID;
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
  public void setContents(ClientID clientID, TransactionID transactionID, EntityDescriptor entityDescriptor, Type type, boolean requiresReplication, byte[] extendedData) {
    // Make sure that this wasn't called twice.
    Assert.assertNull(this.type);
    Assert.assertNotNull(clientID);
    Assert.assertNotNull(transactionID);
    Assert.assertNotNull(entityDescriptor);
    Assert.assertNotNull(type);
    Assert.assertNotNull(extendedData);

    this.clientID = clientID;
    this.transactionID = transactionID;
    this.entityDescriptor = entityDescriptor;
    this.type = type;
    this.requiresReplication = requiresReplication;
    this.extendedData = extendedData;
  }
  
  
  public NetworkVoltronEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public NetworkVoltronEntityMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    @SuppressWarnings("resource")
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair((byte)0, (byte)0);
    
    this.clientID.serializeTo(outputStream);
    
    outputStream.writeLong(this.transactionID.toLong());
    
    this.entityDescriptor.serializeTo(outputStream);
    
    outputStream.writeInt(type.ordinal());
    
    outputStream.writeInt(extendedData.length);
    outputStream.write(extendedData);
  }
  
  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    Assert.assertTrue(0 == name);
    Assert.assertTrue(null == this.clientID);
    // Read our dummy byte.
    getByteValue();
    
    this.clientID = ClientID.readFrom(getInputStream());
    this.transactionID = new TransactionID(getLongValue());
    this.entityDescriptor = EntityDescriptor.readFrom(getInputStream());
    this.type = Type.values()[getIntValue()];
    this.extendedData = getBytesArray();
    return true;
  }
}
