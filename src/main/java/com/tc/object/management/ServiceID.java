/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.management;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public final class ServiceID implements TCSerializable {

  private static final AtomicInteger ID_GEN = new AtomicInteger();

  public static ServiceID newServiceID(Object obj) {
    return new ServiceID(obj.getClass().getName(), ID_GEN.incrementAndGet());
  }

  private String className;
  private int id;

  public ServiceID() {
  }

  public ServiceID(String className, int id) {
    this.className = className;
    this.id = id;
  }

  public String getClassName() {
    return className;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServiceID) {
      ServiceID other = (ServiceID)obj;
      return other.className.equals(className) && other.id == id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return className.hashCode() + id;
  }

  @Override
  public String toString() {
    return "ServiceID[" + className + ":" + id + "]";
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(id);
    serialOutput.writeString(className);
  }

  @Override
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    id = serialInput.readInt();
    className = serialInput.readString();
    return this;
  }
}
