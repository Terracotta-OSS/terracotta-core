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
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityUserException;


public class VoltronEntityAppliedResponseImpl extends DSOMessageBase implements VoltronEntityAppliedResponse {
  private TransactionID transactionID;
  private boolean isSuccess;
  private boolean isRetire;
  private byte[] successResponse;
  private EntityException failureException;
  
  @Override
  public VoltronEntityMessage.Acks getAckType() {
    return VoltronEntityMessage.Acks.APPLIED;
  }
  
  @Override
  public void setSuccess(TransactionID transactionID, byte[] response, boolean retire) {
    Assert.assertNull(this.transactionID);
    Assert.assertNull(this.successResponse);
    Assert.assertNull(this.failureException);
    Assert.assertNotNull(transactionID);
    Assert.assertNotNull(response);
    
    this.transactionID = transactionID;
    this.isSuccess = true;
    this.isRetire = retire;
    this.successResponse = response;
  }
  
  @Override
  public void setFailure(TransactionID transactionID, EntityException exception, boolean retire) {
    Assert.assertNull(this.transactionID);
    Assert.assertNull(this.successResponse);
    Assert.assertNull(this.failureException);
    Assert.assertNotNull(transactionID);
    Assert.assertNotNull(exception);
    
    this.transactionID = transactionID;
    this.isSuccess = false;
    this.isRetire = retire;
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
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair((byte)0, (byte)0);
    
    outputStream.writeLong(this.transactionID.toLong());
    
    outputStream.writeBoolean(this.isSuccess);
    outputStream.writeBoolean(this.isRetire);
    
    if (this.isSuccess) {
      Assert.assertNotNull(this.successResponse);
      outputStream.writeInt(this.successResponse.length);
      outputStream.write(this.successResponse);
    } else {
      Assert.assertNotNull(this.failureException);
      // We need to manually serialize the exception using Java serialization.
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      try {
        ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);      
        try {
          objectOutput.writeObject(this.failureException);
        } finally {
          objectOutput.close();
        }
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
    this.isRetire = getBooleanValue();
    if (this.isSuccess) {
      this.successResponse = getBytesArray();
    } else {
      ByteArrayInputStream byteInput = new ByteArrayInputStream(getBytesArray());
      ObjectInputStream objectInput = new ObjectInputStream(byteInput);
      try {
          this.failureException = (EntityException) objectInput.readObject();
      } catch (ClassNotFoundException e) {
        // We may want to make this into an assertion but we do have a mechanism to pass it up to the next level so wrap
        // it in a user exception.
        this.failureException = new EntityUserException(null, null, e);
      } finally {
        objectInput.close();
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
  public EntityException getFailureException() {
    return this.failureException;
  }
  
  @Override 
  public boolean alsoRetire() {
    return this.isRetire;
  }
}
