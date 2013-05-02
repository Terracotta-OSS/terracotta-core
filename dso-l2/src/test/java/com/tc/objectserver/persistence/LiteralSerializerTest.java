package com.tc.objectserver.persistence;

import com.tc.test.TCTestCase;

import java.nio.ByteBuffer;

/**
 * @author tim
 */
public class LiteralSerializerTest extends TCTestCase {
  public void testNestedStrings() throws Exception {
    ByteBuffer s1 = LiteralSerializer.INSTANCE.transform("foo");
    ByteBuffer s2 = LiteralSerializer.INSTANCE.transform("bar");
    ByteBuffer combined = ByteBuffer.allocate(s1.remaining() + s2.remaining());
    combined.put(s1).put(s2).flip();
    assertEquals("foo", LiteralSerializer.INSTANCE.recover(combined));
    assertEquals("bar", LiteralSerializer.INSTANCE.recover(combined));
  }
}
