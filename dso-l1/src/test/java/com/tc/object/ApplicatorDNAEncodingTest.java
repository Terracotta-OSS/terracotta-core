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
import com.tc.object.loaders.LoaderDescription;
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
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    final byte[] b = new byte[] {};

    final DNAEncoding encoding = getApplicatorEncoding();
    encoding.encode(b, output);

    // The bug this test is for only happens if DNAEncoding gets back -1 from the input stream upon being asked to read
    // 0 bytes from a stream that is at EOF. ByteArrayInputStream happens to be one implemented in such a way
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy(new TCByteBufferInputStream(output.toArray()), baos);
    final TCObjectInputStream input = new TCObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

    assertTrue(Arrays.equals(b, (byte[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  public void testNonPrimitiveArrays2() throws Exception {
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    final Integer[] array = new Integer[] { new Integer(43), new Integer(-1) };
    final ObjectID[] array2 = new ObjectID[] {};

    final DNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(array, output);
    encoding.encodeArray(array2, output);

    final TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertTrue(Arrays.equals(array, (Object[]) encoding.decode(input)));
    assertTrue(Arrays.equals(array2, (Object[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  private DNAEncoding getApplicatorEncoding() {
    return new ApplicatorDNAEncodingImpl(this.classProvider);
  }

  public void testNonPrimitiveArrays() throws Exception {
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    final Object[] array = new Object[] { new ObjectID(12), new Integer(34), new Double(Math.PI), ObjectID.NULL_ID,
        new Long(Long.MIN_VALUE + 34), "timmy" };

    final DNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(array, output);

    final TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertTrue(Arrays.equals(array, (Object[]) encoding.decode(input)));

    assertEquals(0, input.available());
  }

  public void testNullArray() throws Exception {
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    final DNAEncoding encoding = getApplicatorEncoding();
    encoding.encodeArray(null, output);
    final TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

    assertNull(encoding.decode(input));

    assertEquals(0, input.available());
  }

  public void testPrimitiveArrays() throws Exception {

    final DNAEncoding encoding = getApplicatorEncoding();
    for (int iter = 0; iter < 250; iter++) {
      final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

      final byte[] b = makeByteArray();
      final char[] c = makeCharArray();
      final double[] d = makeDoubleArray();
      final float[] f = makeFloatArray();
      final int[] i = makeIntArray();
      final long[] j = makeLongArray();
      final short[] s = makeShortArray();
      final boolean[] z = makeBooleanArray();

      encoding.encodeArray(b, output);
      encoding.encodeArray(c, output);
      encoding.encodeArray(d, output);
      encoding.encodeArray(f, output);
      encoding.encodeArray(i, output);
      encoding.encodeArray(j, output);
      encoding.encodeArray(s, output);
      encoding.encodeArray(z, output);

      final TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());

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
    final short[] rv = new short[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = (short) this.rnd.nextInt();
    }
    return rv;
  }

  private long[] makeLongArray() {
    final long[] rv = new long[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = this.rnd.nextLong();
    }
    return rv;
  }

  private int[] makeIntArray() {
    final int[] rv = new int[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = this.rnd.nextInt();
    }
    return rv;
  }

  private float[] makeFloatArray() {
    final float[] rv = new float[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = this.rnd.nextFloat();
    }

    return rv;
  }

  private double[] makeDoubleArray() {
    final double[] rv = new double[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = this.rnd.nextDouble();
    }

    return rv;
  }

  private char[] makeCharArray() {
    final char[] rv = new char[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = new Character((char) this.rnd.nextInt(Character.MAX_VALUE)).charValue();
    }
    return rv;
  }

  private byte[] makeByteArray() {
    final byte[] rv = new byte[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = (byte) this.rnd.nextInt();
    }
    return rv;
  }

  private boolean[] makeBooleanArray() {
    final boolean[] rv = new boolean[this.rnd.nextInt(10)];
    for (int i = 0; i < rv.length; i++) {
      rv[i] = this.rnd.nextBoolean();
    }
    return rv;
  }

  public void testUnicodeChars() throws Exception {
    final StringBuffer str = new StringBuffer();
    for (int i = 0; i < 100; i++) {
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
    final String littleString = new String("abc");
    helpTestStringEncodingDecoding(littleString, false, false);
  }

  public void testCompressedStringDecoding() throws Exception {
    final String bigString = getBigString(100000);
    helpTestStringEncodingDecoding(bigString, true, false);
  }

  public void testCompressedInternedStringDecoding() throws Exception {
    String bigString = getBigString(100000);
    bigString = StringTCUtil.intern(bigString);
    helpTestStringEncodingDecoding(bigString, true, true);
  }

  public void helpTestStringEncodingDecoding(final String s, final boolean compressed, final boolean interned)
      throws Exception {
    // Encode string using applicator encoding into data
    final DNAEncoding applicatorEncoding = getApplicatorEncoding();
    TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    applicatorEncoding.encode(s, output);
    final TCByteBuffer[] data = output.toArray();

    // Decode string from data using storage encoding (into UTF8ByteDataHolder) into decoded
    final DNAEncoding storageEncoding = getStorageEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    final UTF8ByteDataHolder decoded = (UTF8ByteDataHolder) storageEncoding.decode(input);

    if (compressed) {
      assertTrue(decoded instanceof UTF8ByteCompressedDataHolder);
      final UTF8ByteCompressedDataHolder compressedDecoded = (UTF8ByteCompressedDataHolder) decoded;
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
    final TCByteBuffer[] data2 = output.toArray();

    // Decode UTF8ByteDataHolder from data2 into decoded2 using storage encoding
    input = new TCByteBufferInputStream(data2);
    final UTF8ByteDataHolder decoded2 = (UTF8ByteDataHolder) storageEncoding.decode(input);
    assertEquals(decoded, decoded2);

    // Decode from original data using applicator encoding into str
    input = new TCByteBufferInputStream(data);
    final String str = (String) applicatorEncoding.decode(input);
    assertEquals(s, str);

    // Decode from data2 using applicator encoding into str2
    input = new TCByteBufferInputStream(data2);
    final String str2 = (String) applicatorEncoding.decode(input);
    assertEquals(s, str2);

  }

  private String getBigString(final int length) {
    final String sample = "mold for Big String";
    final StringBuffer sb = new StringBuffer();
    while (sb.length() < length) {
      sb.append(sample);
    }
    return sb.toString();
  }

  public void testStringDecode() throws Exception {
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    DNAEncoding encoding = getApplicatorEncoding();
    encoding.encode("timmy", output);
    UTF8ByteDataHolder orgUTF;
    encoding.encode((orgUTF = new UTF8ByteDataHolder("teck".getBytes("UTF-8"))), output);

    final TCByteBuffer[] data = output.toArray();

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
    final String str1 = new String("Simran");
    UTF8ByteDataHolder utf8Holder;
    String str2;

    str2 = StringTCUtil.intern(str1);
    assertTrue(StringTCUtil.isInterned(str2));

    utf8Holder = new UTF8ByteDataHolder(str2);
    assertTrue(utf8Holder.isInterned());
  }

  public void testStringInternDecode() throws Exception {
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();
    DNAEncoding encoding = getApplicatorEncoding();

    final String orgStr = new String("Life is a interned Circle");
    String str = StringTCUtil.intern(orgStr);
    encoding.encode(str, output);

    final String temp = new String("May be, life is a interned Triangle");
    str = StringTCUtil.intern(temp);
    final UTF8ByteDataHolder orgUTF = new UTF8ByteDataHolder(str);
    encoding.encode(orgUTF, output);

    final String orgNonIntStr = new String("But, life is not a straight line for sure");
    encoding.encode(orgNonIntStr, output);

    final TCByteBuffer[] data = output.toArray();

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
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    DNAEncoding encoding = getApplicatorEncoding();
    encoding.encode(getClass(), output);
    Class c = Object.class;
    final UTF8ByteDataHolder name = new UTF8ByteDataHolder(c.getName());
    final UTF8ByteDataHolder def = new UTF8ByteDataHolder(this.classProvider.getLoaderDescriptionFor(c)
        .toDelimitedString());
    final ClassInstance ci = new ClassInstance(name, def);
    encoding.encode(ci, output);

    final TCByteBuffer[] data = output.toArray();

    encoding = getStorageEncoder();
    TCByteBufferInputStream input = new TCByteBufferInputStream(data);
    ClassInstance holder = (ClassInstance) encoding.decode(input);
    assertEquals(getClass().getName(), holder.getName().asString());
    assertEquals(this.classProvider.getLoaderDescriptionFor(getClass()), LoaderDescription.fromString(holder
        .getLoaderDef().asString()));

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
    final TCByteBufferOutputStream output = new TCByteBufferOutputStream();

    final List data = new ArrayList();
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

    final DNAEncoding encoding = getApplicatorEncoding();
    for (final Iterator i = data.iterator(); i.hasNext();) {
      encoding.encode(i.next(), output);
    }

    final TCByteBufferInputStream input = new TCByteBufferInputStream(output.toArray());
    for (final Iterator i = data.iterator(); i.hasNext();) {
      final Object orig = i.next();
      final Object decoded = encoding.decode(input);

      assertEquals(orig, decoded);
    }

    assertEquals(0, input.available());
  }

}
