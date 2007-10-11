/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.exception.TCRuntimeException;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class TCStackTraceElement implements TCSerializable, Serializable {
  private StackTraceElement[] stackTraceElements;
  private int hashCode;

  public TCStackTraceElement() {
    return;
  }

  public TCStackTraceElement(StackTraceElement[] stackTraceElements) {
    this.stackTraceElements = stackTraceElements;
    
    computeHashCode();
  }
  
  private void computeHashCode() {
    HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(5503, 6737);
    for (int i = 0; i < stackTraceElements.length; i++) {
      hashCodeBuilder.append(stackTraceElements[i].hashCode());
    }
    this.hashCode = hashCodeBuilder.toHashCode();
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    int length = serialInput.readInt();
    stackTraceElements = new StackTraceElement[length];
    for (int i = 0; i < length; i++) {
      int numBytes = serialInput.readInt();
      byte[] bytes = new byte[numBytes];
      serialInput.read(bytes);
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      ObjectInputStream is = new ObjectInputStream(bis);
      try {
        stackTraceElements[i] = (StackTraceElement) is.readObject();
      } catch (ClassNotFoundException e) {
        throw new TCRuntimeException(e);
      }
    }
    computeHashCode();

    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(stackTraceElements.length);
    for (int i = 0; i < stackTraceElements.length; i++) {
      StackTraceElement se = stackTraceElements[i];
      try {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(se);
        byte[] bytes = bos.toByteArray();
        serialOutput.writeInt(bytes.length);
        serialOutput.write(bytes);
      } catch (IOException e) {
        throw new TCRuntimeException(e);
      }
    }
  }
  
  public StackTraceElement[] getStackTraceElements() {
    return this.stackTraceElements;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < stackTraceElements.length; i++) {
      for (int j = 0; j < i; j++) {
        sb.append(" ");
      }
      sb.append(stackTraceElements[i].toString());
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof TCStackTraceElement)) { return false; }
    if (this == obj) { return true; }

    TCStackTraceElement so = (TCStackTraceElement) obj;
    if (this.stackTraceElements.length != so.stackTraceElements.length) { return false; }
    for (int i = 0; i < this.stackTraceElements.length; i++) {
      if (!this.stackTraceElements[i].equals(so.stackTraceElements[i])) { return false; }
    }
    return true;
  }

  public int hashCode() {
    return hashCode;
  }
}
