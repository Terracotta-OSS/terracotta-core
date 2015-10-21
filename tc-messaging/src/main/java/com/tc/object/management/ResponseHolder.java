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
package com.tc.object.management;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

/**
 *
 */
public class ResponseHolder implements TCSerializable<ResponseHolder> {

  private byte[] serializedResponse;
  private byte[] serializedException;

  public ResponseHolder() {
  }

  public ResponseHolder(Exception exception) {
    serializedResponse = new byte[0];
    setException(exception);
  }

  public ResponseHolder(Object response) {
    setResponse(response);
    serializedException = new byte[0];
  }

  public Exception getException(ClassLoader classLoader) throws ClassNotFoundException {
    return (Exception)SerializationHelper.deserialize(serializedException, classLoader);
  }

  public void setException(Exception exception) {
    this.serializedException = SerializationHelper.serialize(exception);
  }

  public Object getResponse(ClassLoader classLoader) throws ClassNotFoundException {
    return SerializationHelper.deserialize(serializedResponse, classLoader);
  }

  public void setResponse(Object response) {
    this.serializedResponse = SerializationHelper.serialize(response);
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    if (serializedResponse == null) {
      serialOutput.writeInt(0);
    } else {
      serialOutput.writeInt(serializedResponse.length);
      serialOutput.write(serializedResponse);
    }

    if (serializedException == null) {
      serialOutput.writeInt(0);
    } else {
      serialOutput.writeInt(serializedException.length);
      serialOutput.write(serializedException);
    }
  }

  @Override
  public ResponseHolder deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    int responseLength = serialInput.readInt();
    serializedResponse = new byte[responseLength];
    serialInput.readFully(serializedResponse);

    int exceptionLength = serialInput.readInt();
    serializedException = new byte[exceptionLength];
    serialInput.readFully(serializedException);

    return this;
  }
}
