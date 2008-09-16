/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io.serializer;

import com.tc.io.serializer.api.BasicSerializer;
import com.tc.io.serializer.api.Serializer;
import com.tc.object.ObjectID;
import com.tc.util.TCAssertionError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

public class DSOSerializerPolicyTest extends TestCase {

  private Serializer serializer;

  public void test() throws Exception {
    DSOSerializerPolicy policy = new DSOSerializerPolicy();
    serializer = new BasicSerializer(policy);

    ObjectID oid = new ObjectID(1);
    assertEquals(oid, serialize(oid));

    Boolean bool = new Boolean(true);
    assertEquals(bool, serialize(bool));

    Byte b = new Byte((byte) 1);
    assertEquals(b, serialize(b));

    Character c = new Character('c');
    assertEquals(c, serialize(c));

    Double d = new Double(1);
    assertEquals(d, serialize(d));

    Float f = new Float(1);
    assertEquals(f, serialize(f));

    Integer i = new Integer(1);
    assertEquals(i, serialize(i));

    Long l = new Long(1);
    assertEquals(l, serialize(l));

    Short s = new Short((short) 1);
    assertEquals(s, serialize(s));

    String string = "orion";
    assertEquals(string, serialize(string));

    Object o = new java.util.Date();
    try {
      assertEquals(o, serialize(o));
      assertTrue(false);
    } catch (TCAssertionError ar) {
      // Normal as Date Objects are not supported natively
    }
  }

  private Object serialize(Object o) throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(bout);

    serializer.serializeTo(o, out);
    out.flush();

    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    TCObjectInputStream in = new TCObjectInputStream(bin);
    return serializer.deserializeFrom(in);
  }
}
