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
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class VoltronEntityAppliedResponseImpl extends DSOMessageBase implements VoltronEntityAppliedResponse {
  private TransactionID transactionID;
  private boolean isSuccess;
  private byte[] successResponse;
  private Exception failureException;
  
  @Override
  public VoltronEntityMessage.Acks getAckType() {
    return VoltronEntityMessage.Acks.APPLIED;
  }
  
  @Override
  public void setSuccess(TransactionID transactionID, byte[] response) {
    Assert.assertNull(this.transactionID);
    Assert.assertNull(this.successResponse);
    Assert.assertNull(this.failureException);
    Assert.assertNotNull(transactionID);
    Assert.assertNotNull(response);
    
    this.transactionID = transactionID;
    this.isSuccess = true;
    this.successResponse = response;
  }
  
  @Override
  public void setFailure(TransactionID transactionID, Exception exception) {
    Assert.assertNull(this.transactionID);
    Assert.assertNull(this.successResponse);
    Assert.assertNull(this.failureException);
    Assert.assertNotNull(transactionID);
    Assert.assertNotNull(exception);
    
    this.transactionID = transactionID;
    this.isSuccess = false;
    this.failureException= exception;
  }
  
  
  public VoltronEntityAppliedResponseImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public VoltronEntityAppliedResponseImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    @SuppressWarnings("resource")
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair((byte)0, (byte)0);
    
    outputStream.writeLong(this.transactionID.toLong());
    
    outputStream.writeBoolean(this.isSuccess);
    
    if (this.isSuccess) {
      Assert.assertNotNull(this.successResponse);
      outputStream.writeInt(this.successResponse.length);
      outputStream.write(this.successResponse);
    } else {
      Assert.assertNotNull(this.failureException);
      // We need to manually serialize the exception using Java serialization.
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      try (ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput)) {
        objectOutput.writeObject(this.failureException);
      } catch (IOException e) {
        throw new AssertionError(e);
      }
      byte[] serializedException = byteOutput.toByteArray();
      outputStream.writeInt(serializedException.length);
      outputStream.write(serializedException);
    }
  }
  
  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    Assert.assertTrue(0 == name);
    Assert.assertTrue(null == this.transactionID);
    // Read our dummy byte.
    getByteValue();
    
    this.transactionID = new TransactionID(getLongValue());
    
    this.isSuccess = getBooleanValue();
    if (this.isSuccess) {
      this.successResponse = getBytesArray();
    } else {
      ByteArrayInputStream byteInput = new ByteArrayInputStream(getBytesArray());
      try (ObjectInputStream objectInput = new ObjectInputStream(byteInput)) {
        this.failureException = (Exception) objectInput.readObject();
      } catch (ClassNotFoundException e) {
        this.failureException = new Exception("Operation failed but exception class not found.", e);
      }
    }
    return true;
  }

  @Override
  public TransactionID getTransactionID() {
    return this.transactionID;
  }

  @Override
  public byte[] getSuccessValue() {
    return this.successResponse;
  }

  @Override
  public Exception getFailureException() {
    return this.failureException;
  }
}
