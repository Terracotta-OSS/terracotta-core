/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import java.io.IOException;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import org.junit.Assert;
import org.junit.Test;

public class LockIdSerializerTest {

  @Test
  public void testStringLockID() {
    StringLockID lock = new StringLockID("FortyTwo");
    Assert.assertEquals(lock, passThrough(lock));
  }

  @Test
  public void testLongLockID() {
    LongLockID lock = new LongLockID(42L);
    Assert.assertEquals(lock, passThrough(lock));
  }

  private LockID passThrough(LockID in) {
    try {
      TCByteBufferOutput tcOut = new TCByteBufferOutputStream();
      try {
        LockIDSerializer serializer = new LockIDSerializer(in);
        serializer.serializeTo(tcOut);
      } finally {
        tcOut.close();
      }

      TCByteBufferInput tcIn = new TCByteBufferInputStream(tcOut.toArray());

      try {
        LockIDSerializer serializer = new LockIDSerializer();
        serializer.deserializeFrom(tcIn);
        return serializer.getLockID();
      } finally {
        tcIn.close();
      }
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  static enum MyEnum {
    A, B, C
  }
}
