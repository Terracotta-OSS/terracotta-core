/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session.util;

import com.terracotta.session.SessionId;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import junit.framework.TestCase;

public class DefaultIdGeneratorTest extends TestCase {

  public final void testIdUniqueness() {
    // this is a pretty silly attempt at uniqueness test...
    HashMap map = new HashMap();
    final String serverId = "SomeServerId";
    final DefaultIdGenerator dig = new DefaultIdGenerator(20, serverId);
    final int expectedLength = 20;
    // I tried running this from 0 to Integer.MAX_VALUE but it's taking a LOOOONG time and ran out of heap space :-(
    for (int i = 0; i < Short.MAX_VALUE; i++) {
      final String id = dig.generateKey();
      assertEquals(expectedLength, id.length());
      assertNull("i = " + i, map.get(id));
      map.put(id, id);
    }
    for (int i = 0; i < 100; i++) {
      final String id = dig.generateKey();
      assertEquals(expectedLength, id.length());
      assertNull("i = " + i, map.get(id));
      map.put(id, id);
    }
  }

  public final void testNextId() {
    final String serverId = "SomeServerId";
    DefaultIdGenerator dig = new DefaultIdGenerator(20, serverId);
    for (short s = Short.MIN_VALUE; true; s++) {
      assertEquals(s, dig.getNextId());
      if (s == Short.MAX_VALUE) break;
    }
    // check loop-back
    assertEquals(Short.MIN_VALUE, dig.getNextId());
  }

  public final void testToBytes() throws IOException {
    for (short s = Short.MIN_VALUE; true; s++) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeShort(s);
      dos.close();
      byte[] bytes = baos.toByteArray();
      byte[] actualBytes = new byte[2];
      DefaultIdGenerator.toBytes(s, actualBytes);
      assertTrue(Arrays.equals(bytes, actualBytes));
      if (s == Short.MAX_VALUE) break;
    }
  }

  public final void testToHex() {
    // test all single byte array
    final byte[] oneByte = new byte[1];
    for (byte b = Byte.MIN_VALUE; true; b++) {
      oneByte[0] = b;
      String out = DefaultIdGenerator.toHex(oneByte, 1);
      String expected = Long.toHexString(0x00000000000000ff & b).toUpperCase();
      expected = (expected.length() < 2) ? "0" + expected : expected;
      assertEquals("failed on byte = " + b, expected, out);
      if (b == Byte.MAX_VALUE) break;
    }

    // test selected multi-byte arrays...
    assertEquals("FFFF", DefaultIdGenerator.toHex(new byte[] { (byte) 0xff, (byte) 0xff }, 2));
    assertEquals("0F0F", DefaultIdGenerator.toHex(new byte[] { (byte) 0x0f, (byte) 0x0f }, 2));
    assertEquals("000F", DefaultIdGenerator.toHex(new byte[] { (byte) 0x00, (byte) 0x0f }, 2));
    assertEquals("01020304050607FF", DefaultIdGenerator.toHex(new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03,
        (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x0ff }, 8));
  }

  public final void testIdLength() {
    DefaultIdGenerator dig = new DefaultIdGenerator(-1234, null);
    assertEquals(8, dig.generateKey().length());

    dig = new DefaultIdGenerator(7, null);
    assertEquals(8, dig.generateKey().length());

    dig = new DefaultIdGenerator(8, null);
    assertEquals(8, dig.generateKey().length());

    dig = new DefaultIdGenerator(9, null);
    assertEquals(9, dig.generateKey().length());

    dig = new DefaultIdGenerator(31, null);
    assertEquals(31, dig.generateKey().length());

    dig = new DefaultIdGenerator(52, null);
    assertEquals(52, dig.generateKey().length());

    dig = new DefaultIdGenerator(666, null);
    assertEquals(666, dig.generateKey().length());

    dig = new DefaultIdGenerator(777, null);
    assertEquals(777, dig.generateKey().length());
  }

  public final void testMakeInstanceFromBrowserId() {
    final String key = "1234567890";
    final String serverId = "1234567890";
    final String newServerId = "someServerId";
    final String browserId = key + "!" + serverId;
    final String externalId = key + "!" + newServerId;
    DefaultIdGenerator dig = new DefaultIdGenerator(10, newServerId);
    SessionId id = dig.makeInstanceFromBrowserId(browserId);
    assertEquals(key, id.getKey());
    assertEquals(browserId, id.getRequestedId());
    assertEquals(externalId, id.getExternalId());
  }
}
