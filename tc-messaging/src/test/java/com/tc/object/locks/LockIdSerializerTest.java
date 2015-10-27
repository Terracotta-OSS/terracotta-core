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
