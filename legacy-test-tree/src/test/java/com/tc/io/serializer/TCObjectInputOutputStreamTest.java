/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io.serializer;

import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TCObjectInputOutputStreamTest extends TCTestCase {

  public TCObjectInputOutputStreamTest() {
    //
  }

  public void testBasic() throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bao = new ByteArrayOutputStream(1024);

    TCObjectOutputStream os = new TCObjectOutputStream(bao);

    ArrayList l = getLiteralObjects();

    os.write(new byte[] { -5, 5 });
    os.write(42);
    os.writeBoolean(true);
    os.writeBoolean(false);
    os.writeByte(11);
    os.writeChar('t');
    os.writeDouble(3.14D);
    os.writeFloat(2.78F);
    os.writeInt(12345678);
    os.writeLong(Long.MIN_VALUE);
    os.writeShort(Short.MAX_VALUE);
    os.writeString("yo yo yo");
    os.writeString(null);
    os.writeString(createString(100000));
    writeObjects(os, l);

    // Now try to write a non Literal Object
    boolean failed = false;
    try {
      os.writeObject(new Object());
      failed = true;
    } catch (AssertionError ae) {
      // this is normal
    }
    Assert.assertFalse(failed);
    try {
      os.writeObject(l);
      failed = true;
    } catch (AssertionError ae) {
      // this is normal
    }
    Assert.assertFalse(failed);

    System.err.println("Now testing TCObjectInputStream...");

    ByteArrayInputStream bis = new ByteArrayInputStream(bao.toByteArray());

    TCObjectInputStream is = new TCObjectInputStream(bis);
    byte[] b = new byte[2];
    Arrays.fill(b, (byte) 69); // these values will be overwritten
    int read = is.read(b);
    assertEquals(2, read);
    assertTrue(Arrays.equals(new byte[] { -5, 5 }, b));
    assertEquals(42, is.read());
    assertEquals(true, is.readBoolean());
    assertEquals(false, is.readBoolean());
    assertEquals(11, is.readByte());
    assertEquals('t', is.readChar());
    assertEquals(Double.doubleToLongBits(3.14D), Double.doubleToLongBits(is.readDouble()));
    assertEquals(Float.floatToIntBits(2.78F), Float.floatToIntBits(is.readFloat()));
    assertEquals(12345678, is.readInt());
    assertEquals(Long.MIN_VALUE, is.readLong());
    assertEquals(Short.MAX_VALUE, is.readShort());
    assertEquals("yo yo yo", is.readString());
    assertEquals(null, is.readString());
    assertEquals(createString(100000), is.readString());
    assertEquals(l, readObjects(is, new ArrayList()));
  }

  private void writeObjects(TCObjectOutputStream os, ArrayList l) {
    os.writeInt(l.size());
    for (Iterator i = l.iterator(); i.hasNext();) {
      Object element = i.next();
      os.writeObject(element);
    }
  }

  private List readObjects(TCObjectInputStream is, ArrayList l) throws IOException, ClassNotFoundException {
    int count = is.readInt();
    for (int i = 0; i < count; i++) {
      l.add(is.readObject());
    }
    return l;
  }

  private ArrayList getLiteralObjects() {
    ArrayList l = new ArrayList();
    l.add(new Integer(1009));
    l.add("Hello there ");
    l.add(new Long(909999999));
    l.add(new Double(99999999.9374899999d));
    l.add(new Float(9034699.9374899999f));
    l.add(new Short((short) 0x45));
    l.add(new Character('S'));
    l.add(new ObjectID(38745488234l));
    l.add(new Byte((byte) 77));
    l.add(new Boolean(true));
    l.add(this.getClass());
    l.add(new UTF8ByteDataHolder("Hello back"));
    l.add(new ClassInstance(new UTF8ByteDataHolder(this.getClass().getName())));
    // Object o[] = new Object[] { new ObjectID(88), new ObjectID(77), new Integer(66), new Long(55) };
    // l.add(o);
    return l;
  }

  private static String createString(int length) {
    char[] chars = new char[length];
    Arrays.fill(chars, 't');
    return new String(chars);
  }

}
