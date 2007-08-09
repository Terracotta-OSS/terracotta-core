/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import org.apache.commons.io.IOUtils;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.IDNAEncoding;
import com.tc.object.loaders.ClassProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class DNAEncodingTest extends TestCase {

  Random        rnd           = new Random();
  ClassProvider classProvider = new MockClassProvider();

  public void testZeroLengthByteArray() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    byte[] b = new byte[] {};

    IDNAEncoding encoding = getApplicatorEncoding();
    encoding.encode(b, output);

    // The bug this test is for only happens if DNAEncoding gets back -1 from the input stream upon being asked to read
    // 0 bytes from a stream that is at EOF. ByteArrayInputStream happens to be one implemented in such a way
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy(new TCByteBufferInputStream(output.toArray()), baos);
    TCObjectInputStream input = new TCObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

    assertTrue(Arrays.equals(b, (byte[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  public void testNonPrimitiveArrays2() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    Integer[] array = new Integer[] { new Integer(43), new Integer(-1) };
    ObjectID[] array2 = new ObjectID[] {};

    IDNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(array, output);
    encoding.encodeArray(array2, output);

    TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertTrue(Arrays.equals(array, (Object[]) encoding.decode(input)));
    assertTrue(Arrays.equals(array2, (Object[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  private IDNAEncoding getApplicatorEncoding() {
    return new DNAEncoding(classProvider);
  }

  public void testNonPrimitiveArrays() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    Object[] array = new Object[] { new ObjectID(12), new Integer(34), new Double(Math.PI), ObjectID.NULL_ID,
        new Long(Long.MIN_VALUE + 34), "timmy" };

    IDNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(array, output);

    TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertTrue(Arrays.equals(array, (Object[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  public void testNullArray() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    IDNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(null, output);
    TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertNull(encoding.decode(input));

    assertEquals(0, input.available());
  }

  public void testPrimitiveArrays() throws Exception {

    IDNAEncoding encoding = getApplicatorEncoding();
    for (int iter = 0; iter < 250; iter++) {
      TCByteBufferOutputStream output = new TCByteBufferOutputStream();

      byte[] b = makeByteArray();
      char[] c = makeCharArray();
      double[] d = makeDoubleArray();
      float[] f = makeFloatArray();
      int[] i = makeIntArray();
      long[] j = makeLongArray();
      short[] s = makeShortArray();
      boolean[] z = makeBooleanArray();

      encoding.encodeArray(b, output);
      encoding.encodeArray(c, output);
      encoding.encodeArray(d, output);
      encoding.encodeArray(f, output);
      encoding.encodeArray(i, output);
      encoding.encodeArray(j, output);
      encoding.encodeArray(s, output);
      encoding.encodeArray(z, output);

      TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

      assertTrue(Arrays.equals(b, (byte[]) encoding.decode(input)));
      assertTrue(Arrays.equals(c, (char[]) encoding.decode(input)));
      assertTrue(Arrays.equals(d, (double[]) encoding.decode(input)));
      assertTrue(Arrays.equals(f, (float[]) encoding.decode(input)));
      assertTrue(Arrays.equals(i, (int[]) encoding.decode(input)));
      assertTrue(Arrays.equals(j, (long[]) encoding.decode(input)));
      assertTrue(Arrays.equals(s, (short[]) encoding.decode(input)));
      assertTrue(Arrays.equals(z, (boolean[]) encoding.decode(input)));

      assertEquals(0, input.available());
    }
  }

  private short[] makeShortArray() {
    short[] rv = new short[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = (short) rnd.nextInt();
    }
    return rv;
  }

  private long[] makeLongArray() {
    long[] rv = new long[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = rnd.nextLong();
    }
    return rv;
  }

  private int[] makeIntArray() {
    int[] rv = new int[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = rnd.nextInt();
    }
    return rv;
  }

  private float[] makeFloatArray() {
    float[] rv = new float[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = rnd.nextFloat();
    }

    return rv;
  }

  private double[] makeDoubleArray() {
    double[] rv = new double[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = rnd.nextDouble();
    }

    return rv;
  }

  private char[] makeCharArray() {
    char[] rv = new char[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new Character((char) rnd.nextInt(Character.MAX_VALUE)).charValue();
    }
    return rv;
  }

  private byte[] makeByteArray() {
    byte[] rv = new byte[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = (byte) rnd.nextInt();
    }
    return rv;
  }

  private boolean[] makeBooleanArray() {
    boolean[] rv = new boolean[rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = rnd.nextBoolean();
    }
    return rv;
  }

  public void testStringDecode() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    IDNAEncoding encoding = getApplicatorEncoding();
    encoding.encode("timmy", output);
    UTF8ByteDataHolder orgUTF;
    encoding.encode((orgUTF = new UTF8ByteDataHolder("teck".getBytes("UTF-8"))), output);

    TCByteBuffer[] data = output.toArray();

    encoding = getStorageEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    UTF8ByteDataHolder decoded = (UTF8ByteDataHolder) encoding.decode(input);
    assertTrue(Arrays.equals("timmy".getBytes("UTF-8"), decoded.getBytes()));
    decoded = (UTF8ByteDataHolder) encoding.decode(input);
    assertTrue(Arrays.equals("teck".getBytes("UTF-8"), decoded.getBytes()));
    assertEquals(0, input.available());

    encoding = getApplicatorEncoding();
    input = new TCByteBufferInputStream(data);
    String str = (String) encoding.decode(input);
    assertEquals("timmy", str);
    str = (String) encoding.decode(input);
    assertEquals("teck", str);
    assertEquals(0, input.available());

    encoding = getSerializerEncoder();
    input = new TCByteBufferInputStream(data);
    str = (String) encoding.decode(input);
    assertEquals("timmy", str);
    decoded = (UTF8ByteDataHolder) encoding.decode(input);
    assertEquals(orgUTF, decoded);
    assertEquals(0, input.available());
  }

  private IDNAEncoding getStorageEncoder() {
    return new DNAEncoding(IDNAEncoding.STORAGE);
  }

  private IDNAEncoding getSerializerEncoder() {
    return new DNAEncoding(IDNAEncoding.SERIALIZER);
  }

  public void testClassExpand() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    IDNAEncoding encoding = getApplicatorEncoding();
    encoding.encode(getClass(), output);
    Class c = Object.class;
    UTF8ByteDataHolder name = new UTF8ByteDataHolder(c.getName());
    UTF8ByteDataHolder def = new UTF8ByteDataHolder(classProvider.getLoaderDescriptionFor(c));
    ClassInstance ci = new ClassInstance(name, def);
    encoding.encode(ci, output);

    TCByteBuffer[] data = output.toArray();

    encoding = getStorageEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    ClassInstance holder = (ClassInstance) encoding.decode(input);
    assertEquals(getClass().getName(), holder.getName().asString());
    assertEquals(classProvider.getLoaderDescriptionFor(getClass()), holder.getLoaderDef().asString());

    holder = (ClassInstance) encoding.decode(input);
    assertEquals(name, holder.getName());
    assertEquals(def, holder.getLoaderDef());

    assertEquals(0, input.available());

    encoding = getApplicatorEncoding();
    input = new TCByteBufferInputStream(data);
    c = (Class) encoding.decode(input);
    assertEquals(getClass(), c);
    c = (Class) encoding.decode(input);
    assertEquals(Object.class, c);
    assertEquals(0, input.available());

  }

  public void testClassSerialize() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    IDNAEncoding encoding = getSerializerEncoder();
    encoding.encode(getClass(), output);
    Class c = Object.class;
    UTF8ByteDataHolder name = new UTF8ByteDataHolder(c.getName());
    UTF8ByteDataHolder def = new UTF8ByteDataHolder(classProvider.getLoaderDescriptionFor(c));
    ClassInstance ci = new ClassInstance(name, def);
    encoding.encode(ci, output);

    TCByteBuffer[] data = output.toArray();

    encoding = getSerializerEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    c = (Class) encoding.decode(input);
    assertEquals(getClass(), c);
    ClassInstance holder = (ClassInstance) encoding.decode(input);
    assertEquals(ci, holder);
    assertEquals(0, input.available());
  }

  public void testBasic() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    List data = new ArrayList();
    data.add(new ObjectID(1));
    data.add("one");
    data.add(new Boolean(true));
    data.add("two");
    data.add(new Byte((byte) 42));
    data.add("three");
    data.add(new Character('\t'));
    data.add("four");
    data.add(new Double(Math.PI));
    data.add("five");
    data.add(new Float(Math.E));
    data.add("six");
    data.add(new Integer(Integer.MAX_VALUE));
    data.add("seven");
    data.add(new Long(System.currentTimeMillis() % 17));
    data.add("eight");
    data.add(new Short((short) -1));
    data.add("nine");
    data.add(new BigInteger(512, new Random()));
    data.add("ten");
    data.add(new BigDecimal(84564547.45465478d));

    IDNAEncoding encoding = getApplicatorEncoding();
    for (Iterator i = data.iterator(); i.hasNext();) {
      encoding.encode(i.next(), output);
    }

    TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());
    for (Iterator i = data.iterator(); i.hasNext();) {
      Object orig = i.next();
      Object decoded = encoding.decode(input);

      assertEquals(orig, decoded);
    }

    assertEquals(0, input.available());
  }

}
