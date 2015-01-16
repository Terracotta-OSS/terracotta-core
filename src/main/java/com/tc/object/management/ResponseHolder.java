/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public Exception getException(final ClassLoader classLoader) throws ClassNotFoundException {
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
