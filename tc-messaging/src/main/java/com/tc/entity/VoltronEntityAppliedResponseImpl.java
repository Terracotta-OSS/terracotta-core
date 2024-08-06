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

import com.tc.exception.ServerExceptionType;
import com.tc.exception.ServerException;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.EntityID;
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
  private static byte HYDRATE_EXCEPTION_HANDLING_VERSION_NAME = 0;
  private static byte HYDRATE_EXCEPTION_HANDLING_VERSION_0 = 0;
  private static byte HYDRATE_EXCEPTION_HANDLING_VERSION_1 = 1;
  
  @Override
  public VoltronEntityMessage.Acks getAckType() {
    return VoltronEntityMessage.Acks.COMPLETED;
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
  public void setFailure(TransactionID transactionID, ServerException exception) {
    Assert.assertNull(this.transactionID);
    Assert.assertNull(this.successResponse);
    Assert.assertNull(this.failureException);
    Assert.assertNotNull(transactionID);
    Assert.assertNotNull(exception);
    
    this.transactionID = transactionID;
    this.isSuccess = false;
    this.failureException = exception;
  }
  
  
  public VoltronEntityAppliedResponseImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public VoltronEntityAppliedResponseImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair(HYDRATE_EXCEPTION_HANDLING_VERSION_NAME, HYDRATE_EXCEPTION_HANDLING_VERSION_1);
    
    outputStream.writeLong(this.transactionID.toLong());
    
    outputStream.writeBoolean(this.isSuccess);
    
    if (this.isSuccess) {
      Assert.assertNotNull(this.successResponse);
      outputStream.writeInt(this.successResponse.length);
      outputStream.write(this.successResponse);
    } else {
      Assert.assertNotNull(this.failureException);
      outputStream.writeInt(((ServerException)failureException).getType().ordinal());
      outputStream.writeString(((ServerException)failureException).getClassName());
      outputStream.writeString(((ServerException)failureException).getEntityName());
      outputStream.writeString(failureException.getMessage());      
      Throwable cause = failureException.getCause();
      if (cause != null) {
        StackTraceElement[] stack = cause.getStackTrace();
        // We need to manually serialize the exception using Java serialization.
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput)) {      
          objectOutput.writeObject(stack);
        } catch (IOException ioe) {

        }
        byte[] serializedException = byteOutput.toByteArray();
        outputStream.writeInt(serializedException.length);
        outputStream.write(serializedException);
      } else {
        outputStream.writeInt(0);
      }
    }
  }
  
  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    Assert.assertTrue(0 == name);
    Assert.assertTrue(null == this.transactionID);
    // Read our dummy byte.
    int version = getByteValue();
    
    this.transactionID = new TransactionID(getLongValue());
    
    this.isSuccess = getBooleanValue();

    if (this.isSuccess) {
      this.successResponse = getBytesArray();
    } else {
      if (version == HYDRATE_EXCEPTION_HANDLING_VERSION_1) {
        ServerExceptionType type = ServerExceptionType.values()[getIntValue()];
        String cname = getStringValue();
        String ename = getStringValue();
        String description = getStringValue();
        byte[] objStream = getBytesArray();
        StackTraceElement[] cause = null;
        if (objStream.length > 0) {
          ByteArrayInputStream byteInput = new ByteArrayInputStream(objStream);
          try (ObjectInputStream objectInput = new ObjectInputStream(byteInput)) {
            cause = (StackTraceElement[])objectInput.readObject();
          } catch (ClassNotFoundException | IOException e) {
          // ignore
          }
        }
        this.failureException = ServerException.hydrateException(new EntityID(cname,ename), description, type, cause);
      } else if (version == HYDRATE_EXCEPTION_HANDLING_VERSION_0) {
        ByteArrayInputStream byteInput = new ByteArrayInputStream(getBytesArray());
        try (ObjectInputStream objectInput = new ObjectInputStream(byteInput)) {
            this.failureException = (Exception)objectInput.readObject();
        } catch (ClassNotFoundException e) {
          // We may want to make this into an assertion but we do have a mechanism to pass it up to the next level so wrap
          // it in a user exception.
          this.failureException =  e;
        }
      } else {
        throw new IOException("unknown exception handling version");
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
