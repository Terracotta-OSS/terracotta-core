/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.TestCase;

public class LockIdSerializerTest extends TestCase {

  public void testDsoLockID() {
    DsoLockID lock = new DsoLockID(new ObjectID(42L));
    Assert.assertEquals(lock, passThrough(lock));
  }

  public void testVolatileLockID() {
    DsoVolatileLockID lock = new DsoVolatileLockID(new ObjectID(42L), "theMeaning");
    Assert.assertEquals(lock, passThrough(lock));
  }

  public void testStringLockID() {
    StringLockID lock = new StringLockID("FortyTwo");
    Assert.assertEquals(lock, passThrough(lock));
  }

  public void testLongLockID() {
    LongLockID lock = new LongLockID(42L);
    Assert.assertEquals(lock, passThrough(lock));
  }

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
    DsoLiteralLockID lock = new DsoLiteralLockID(manager, literal);
    Assert.assertEquals(lock, passThrough(lock));
  }

  public void unclusteredLockTest(Object literal) {
    try {
      new DsoLiteralLockID(manager, literal);
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

  static Manager manager = (Manager) Proxy.newProxyInstance(LockIDSerializer.class.getClassLoader(),
                                                            new Class[] { Manager.class }, new DumbClassProvider());

  static class DumbClassProvider implements ClassProvider, InvocationHandler {

    static ClassLoader CLASS_LOADER = LockIDSerializer.class.getClassLoader();

    public Class getClassFor(String className) {
      throw new AssertionError();
    }

    /*
     * Copied from ManagerImpl
     */
    public boolean isLiteralAutolock(final Object o) {
      if (o instanceof Manageable) { return false; }
      return (!(o instanceof Class)) && (!(o instanceof ObjectID)) && LiteralValues.isLiteralInstance(o);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("getClassProvider".equals(method.getName())) {
        return this;
      } else if ("isLiteralAutolock".equals(method.getName())) {
        return isLiteralAutolock(args[0]);
      } else {
        throw new AssertionError("Cannot handle " + method);
      }
    }

  }
}
