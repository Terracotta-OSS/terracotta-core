/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import java.io.IOException;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;

import org.junit.Assert;
import org.junit.Test;

public class LockIdSerializerTest {

  @Test
  public void testDsoLockID() {
    DsoLockID lock = new DsoLockID(new ObjectID(42L));
    Assert.assertEquals(lock, passThrough(lock));
  }

  @Test
  public void testVolatileLockID() {
    DsoVolatileLockID lock = new DsoVolatileLockID(new ObjectID(42L), "theMeaning");
    Assert.assertEquals(lock, passThrough(lock));
  }

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

  @Test
  public void testLiteralLockID() {
    literalLockTest(Integer.valueOf(42));
    literalLockTest(Long.valueOf(42));
    literalLockTest(Character.valueOf((char) 42));
    literalLockTest(Float.valueOf(42f));
    literalLockTest(Double.valueOf(42d));
    literalLockTest(Byte.valueOf((byte) 42));
    literalLockTest(Boolean.valueOf(true));
    literalLockTest(Short.valueOf((short) 42));

    literalLockTest(MyEnum.A);

    try {
      literalLockTest("bad string!");
      throw new IllegalStateException();
    } catch (AssertionError e) {
      // expected
    }

    try {
      literalLockTest(Object.class);
      throw new IllegalStateException();
    } catch (AssertionError e) {
      // expected
    }

    try {
      literalLockTest(new ObjectID(42));
      throw new IllegalStateException();
    } catch (AssertionError e) {
      // expected
    }
  }

  public void literalLockTest(Object literal) {
    DsoLiteralLockID lock = new DsoLiteralLockID(literal);
    Assert.assertEquals(lock, passThrough(lock));
  }

  @SuppressWarnings("unused")
  public void unclusteredLockTest(Object literal) {
    try {
      new DsoLiteralLockID(literal);
      Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
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
