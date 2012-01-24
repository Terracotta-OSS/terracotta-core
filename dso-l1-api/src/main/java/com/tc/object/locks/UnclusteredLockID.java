/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

import java.io.IOException;

public class UnclusteredLockID implements LockID {

  public static final UnclusteredLockID UNCLUSTERED_LOCK_ID = new UnclusteredLockID();

  private UnclusteredLockID() {
    //
  }

  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    throw new AssertionError("UnclusteredLockID instances should not be being serialized");
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) {
    throw new AssertionError("UnclusteredLockID instances should not be being serialized");
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    throw new AssertionError("UnclusteredLockID instances should not be being serialized");
  }

  @SuppressWarnings("unused")
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    throw new AssertionError("UnclusteredLockID instances should not be being serialized");
  }

  @SuppressWarnings("unused")
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    throw new AssertionError("UnclusteredLockID instances should not be being serialized");
  }

  public int compareTo(final Object o) {
    throw new AssertionError("UnclusteredLockID should not be compared or stored into sorted collections");
  }

}
