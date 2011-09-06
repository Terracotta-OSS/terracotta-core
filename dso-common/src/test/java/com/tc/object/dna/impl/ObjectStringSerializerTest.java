/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class ObjectStringSerializerTest extends TestCase {

  public void test() throws IOException {
    // write it
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    ObjectStringSerializer ser = new ObjectStringSerializerImpl();

    ser.writeFieldName(out, "className.fieldName");
    ser.writeString(out, "timmy teck");
    ser.writeFieldName(out, "className.fieldName");
    ser.writeString(out, "timmy teck");

    // cook it
    TCByteBufferOutputStream data = new TCByteBufferOutputStream();
    ser.serializeTo(data);
    data.write(out.toArray());

    // read it
    TCByteBufferInputStream in = new TCByteBufferInputStream(data.toArray());
    ObjectStringSerializer ser2 = new ObjectStringSerializerImpl();
    ser2.deserializeFrom(in);

    String fn1 = ser2.readFieldName(in);
    String s1 = ser2.readString(in);
    String fn2 = ser2.readFieldName(in);
    String s2 = ser2.readString(in);

    assertEquals("className.fieldName", fn1);
    assertEquals("timmy teck", s1);
    assertEquals("className.fieldName", fn2);
    assertEquals("timmy teck", s2);
  }

  public void testBytes() throws IOException {
    // write it
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    ObjectStringSerializer ser = new ObjectStringSerializerImpl();

    byte[] bytes = new byte[] { 1, 2, 3, 4 };
    ser.writeStringBytes(out, bytes);

    // cook it
    TCByteBufferOutputStream data = new TCByteBufferOutputStream();
    ser.serializeTo(data);
    data.write(out.toArray());

    // read it
    TCByteBufferInputStream in = new TCByteBufferInputStream(data.toArray());
    ObjectStringSerializer ser2 = new ObjectStringSerializerImpl();
    ser2.deserializeFrom(in);

    byte[] read = ser2.readStringBytes(in);

    assertTrue(Arrays.equals(bytes, read));
  }
}
