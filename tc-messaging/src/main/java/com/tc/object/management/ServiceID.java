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
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public final class ServiceID implements TCSerializable<ServiceID> {

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
  public ServiceID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    id = serialInput.readInt();
    className = serialInput.readString();
    return this;
  }
}
