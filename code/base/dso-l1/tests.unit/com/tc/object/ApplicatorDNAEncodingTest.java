/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import org.apache.commons.io.IOUtils;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.ClassInstance;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.StorageDNAEncodingImpl;
import com.tc.object.dna.impl.UTF8ByteCompressedDataHolder;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.StringTCUtil;

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

public class ApplicatorDNAEncodingTest extends TestCase {

  Random        rnd           = new Random();
  ClassProvider classProvider = new MockClassProvider();

  public void testZeroLengthByteArray() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    byte[] b = new byte[] {};

    DNAEncoding encoding = getApplicatorEncoding();
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

    DNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(array, output);
    encoding.encodeArray(array2, output);

    TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertTrue(Arrays.equals(array, (Object[]) encoding.decode(input)));
    assertTrue(Arrays.equals(array2, (Object[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  private DNAEncoding getApplicatorEncoding() {
    return new ApplicatorDNAEncodingImpl(classProvider);
  }

  public void testNonPrimitiveArrays() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    Object[] array = new Object[] { new ObjectID(12), new Integer(34), new Double(Math.PI), ObjectID.NULL_ID,
        new Long(Long.MIN_VALUE + 34), "timmy" };

    DNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(array, output);

    TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertTrue(Arrays.equals(array, (Object[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  public void testNullArray() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    DNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(null, output);
    TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertNull(encoding.decode(input));

    assertEquals(0, input.available());
  }

  public void testPrimitiveArrays() throws Exception {

    DNAEncoding encoding = getApplicatorEncoding();
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
  
  public void testUnicodeChars() throws Exception {
    StringBuffer str = new StringBuffer();
    for(int i=0; i<100; i++) {
      str.append('\u7aba');
      str.append('\ucabb');
      str.append('\uffcf');
      str.append('\ufeb5');
      str.append('\ud0e6');
      str.append('\u204b');
    }
    helpTestStringEncodingDecoding(str.toString(), true, false);
  }
  
  public void testUncompressedInternedStringDecoding() throws Exception {
    String littleString = new String("abc");
    littleString = StringTCUtil.intern(littleString);
    helpTestStringEncodingDecoding(littleString, false, true);
  }

  public void testUncompressedStringDecoding() throws Exception {
    String littleString = new String("abc");
    helpTestStringEncodingDecoding(littleString, false, false);
  }

  public void testCompressedStringDecoding() throws Exception {
    String bigString = getBigString(100000);
    helpTestStringEncodingDecoding(bigString, true, false);
  }

  public void testCompressedInternedStringDecoding() throws Exception {
    String bigString = getBigString(100000);
    bigString = StringTCUtil.intern(bigString);
    helpTestStringEncodingDecoding(bigString, true, true);
  }

  public void helpTestStringEncodingDecoding(String s, boolean compressed, boolean interned) throws Exception {
    // Encode string using applicator encoding into data
    DNAEncoding applicatorEncoding = getApplicatorEncoding();
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    applicatorEncoding.encode(s, output);
    TCByteBuffer[] data = output.toArray();

    // Decode string from data using storage encoding (into UTF8ByteDataHolder) into decoded
    DNAEncoding storageEncoding = getStorageEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    UTF8ByteDataHolder decoded = (UTF8ByteDataHolder) storageEncoding.decode(input);

    if (compressed) {
      assertTrue(decoded instanceof UTF8ByteCompressedDataHolder);
      UTF8ByteCompressedDataHolder compressedDecoded = (UTF8ByteCompressedDataHolder)decoded;
      assertEquals(s.getBytes("UTF-8").length, compressedDecoded.getUncompressedStringLength());
      System.err.println("Compressed String length = " + compressedDecoded.getBytes().length);
      assertEquals(s.length(), compressedDecoded.getStringLength());
      assertEquals(s.hashCode(), compressedDecoded.getStringHash());
    }
    assertEquals(interned, decoded.isInterned());
    assertEquals(s, decoded.asString());

    // Encode UTF8ByteDataHolder into data2 using storage encoding
    output = new TCByteBufferOutputStream();
    storageEncoding.encode(decoded, output);
    TCByteBuffer[] data2 = output.toArray();

    // Decode UTF8ByteDataHolder from data2 into decoded2 using storage encoding
    input = new TCByteBufferInputStream(data2);
    UTF8ByteDataHolder decoded2 = (UTF8ByteDataHolder) storageEncoding.decode(input);
    assertEquals(decoded, decoded2);

    // Decode from original data using applicator encoding into str
    input = new TCByteBufferInputStream(data);
    String str = (String) applicatorEncoding.decode(input);
    assertEquals(s, str);

    // Decode from data2 using applicator encoding into str2
    input = new TCByteBufferInputStream(data2);
    String str2 = (String) applicatorEncoding.decode(input);
    assertEquals(s, str2);

  }

  private String getBigString(int length) {
    String sample = "mold for Big String";
    StringBuffer sb = new StringBuffer();
    while (sb.length() < length) {
      sb.append(sample);
    }
    return sb.toString();
  }

  public void testStringDecode() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    DNAEncoding encoding = getApplicatorEncoding();
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

  public void testStringInternAdaptedFlags() {
    String str1 = new String("Simran");
    UTF8ByteDataHolder utf8Holder;
    String str2;

    str2 = StringTCUtil.intern(str1);
    assertTrue(StringTCUtil.isInterned(str2));

    utf8Holder = new UTF8ByteDataHolder(str2);
    assertTrue(utf8Holder.isInterned());
  }

  public void testStringInternDecode() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    DNAEncoding encoding = getApplicatorEncoding();

    String orgStr = new String("Life is a interned Circle");
    String str = StringTCUtil.intern(orgStr);
    encoding.encode(str, output);

    String temp = new String("May be, life is a interned Triangle");
    str = StringTCUtil.intern(temp);
    UTF8ByteDataHolder orgUTF = new UTF8ByteDataHolder(str);
    encoding.encode(orgUTF, output);

    String orgNonIntStr = new String("But, life is not a straight line for sure");
    encoding.encode(orgNonIntStr, output);

    TCByteBuffer[] data = output.toArray();

    encoding = getStorageEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    UTF8ByteDataHolder decoded = (UTF8ByteDataHolder) encoding.decode(input);
    assertTrue(decoded.isInterned());
    assertTrue(Arrays.equals(orgStr.getBytes("UTF-8"), decoded.getBytes()));
    decoded = (UTF8ByteDataHolder) encoding.decode(input);
    assertTrue(decoded.isInterned());
    assertTrue(Arrays.equals(orgUTF.getBytes(), decoded.getBytes()));
    decoded = (UTF8ByteDataHolder) encoding.decode(input);
    assertFalse(decoded.isInterned());
    assertTrue(Arrays.equals(orgNonIntStr.getBytes("UTF-8"), decoded.getBytes()));
    assertEquals(0, input.available());

    encoding = getApplicatorEncoding();
    input = new TCByteBufferInputStream(data);
    str = (String) encoding.decode(input);
    assertEquals(orgStr, str);
    assertTrue(StringTCUtil.isInterned(str));
    str = (String) encoding.decode(input);
    assertEquals(orgUTF.asString(), str);
    assertTrue(StringTCUtil.isInterned(str));
    str = (String) encoding.decode(input);
    assertEquals(orgNonIntStr, str);
    assertFalse(StringTCUtil.isInterned(str));
    assertEquals(0, input.available());

    encoding = getSerializerEncoder();
    input = new TCByteBufferInputStream(data);
    str = (String) encoding.decode(input);
    assertEquals(orgStr, str);
    assertTrue(StringTCUtil.isInterned(str));
    decoded = (UTF8ByteDataHolder) encoding.decode(input);
    assertEquals(orgUTF, decoded);
    assertTrue(decoded.isInterned());
    str = (String) encoding.decode(input);
    assertEquals(orgNonIntStr, str);
    assertFalse(StringTCUtil.isInterned(str));
    assertEquals(0, input.available());
  }

  private DNAEncoding getStorageEncoder() {
    return new StorageDNAEncodingImpl();
  }

  private DNAEncoding getSerializerEncoder() {
    return new SerializerDNAEncodingImpl();
  }

  public void testClassExpand() throws Exception {
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    DNAEncoding encoding = getApplicatorEncoding();
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

    DNAEncoding encoding = getApplicatorEncoding();
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
