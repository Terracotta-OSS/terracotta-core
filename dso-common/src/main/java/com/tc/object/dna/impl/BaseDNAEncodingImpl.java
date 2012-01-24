/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.compression.CompressedData;
import com.tc.object.compression.StringCompressionUtil;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.loaders.ClassProvider;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import gnu.trove.TObjectIntHashMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.zip.InflaterInputStream;

/**
 * Utility for encoding/decoding DNA
 */
public abstract class BaseDNAEncodingImpl implements DNAEncodingInternal {

  // XXX: These warning thresholds should be done in a non-static way so they can be made configurable
  // and architecture sensitive.
  private static final int                    WARN_THRESHOLD                       = 8 * 1000 * 1000;
  private static final int                    BOOLEAN_WARN                         = WARN_THRESHOLD / 1;
  private static final int                    BYTE_WARN                            = WARN_THRESHOLD / 1;
  private static final int                    CHAR_WARN                            = WARN_THRESHOLD / 2;
  private static final int                    DOUBLE_WARN                          = WARN_THRESHOLD / 8;
  private static final int                    FLOAT_WARN                           = WARN_THRESHOLD / 4;
  private static final int                    INT_WARN                             = WARN_THRESHOLD / 4;
  private static final int                    LONG_WARN                            = WARN_THRESHOLD / 8;
  private static final int                    SHORT_WARN                           = WARN_THRESHOLD / 2;
  private static final int                    REF_WARN                             = WARN_THRESHOLD / 4;

  static final byte                           LOGICAL_ACTION_TYPE                  = 1;
  static final byte                           PHYSICAL_ACTION_TYPE                 = 2;
  static final byte                           ARRAY_ELEMENT_ACTION_TYPE            = 3;
  static final byte                           ENTIRE_ARRAY_ACTION_TYPE             = 4;
  static final byte                           LITERAL_VALUE_ACTION_TYPE            = 5;
  static final byte                           PHYSICAL_ACTION_TYPE_REF_OBJECT      = 6;
  static final byte                           SUB_ARRAY_ACTION_TYPE                = 7;
  static final byte                           META_DATA_ACTION_TYPE                = 8;

  private static final TCLogger               logger                               = TCLogging
                                                                                       .getLogger(BaseDNAEncodingImpl.class);

  protected static final byte                 TYPE_ID_REFERENCE                    = 1;
  protected static final byte                 TYPE_ID_BOOLEAN                      = 2;
  protected static final byte                 TYPE_ID_BYTE                         = 3;
  protected static final byte                 TYPE_ID_CHAR                         = 4;
  protected static final byte                 TYPE_ID_DOUBLE                       = 5;
  protected static final byte                 TYPE_ID_FLOAT                        = 6;
  protected static final byte                 TYPE_ID_INT                          = 7;
  protected static final byte                 TYPE_ID_LONG                         = 10;
  protected static final byte                 TYPE_ID_SHORT                        = 11;
  protected static final byte                 TYPE_ID_STRING                       = 12;
  protected static final byte                 TYPE_ID_STRING_BYTES                 = 13;
  protected static final byte                 TYPE_ID_ARRAY                        = 14;
  protected static final byte                 TYPE_ID_JAVA_LANG_CLASS              = 15;
  protected static final byte                 TYPE_ID_JAVA_LANG_CLASS_HOLDER       = 16;
  protected static final byte                 TYPE_ID_JAVA_LANG_CLASSLOADER        = 20;
  protected static final byte                 TYPE_ID_JAVA_LANG_CLASSLOADER_HOLDER = 21;
  protected static final byte                 TYPE_ID_ENUM                         = 22;
  protected static final byte                 TYPE_ID_ENUM_HOLDER                  = 23;
  protected static final byte                 TYPE_ID_STRING_COMPRESSED            = 25;
  // protected static final byte TYPE_ID_URL = 26;

  private static final byte                   ARRAY_TYPE_PRIMITIVE                 = 1;
  private static final byte                   ARRAY_TYPE_NON_PRIMITIVE             = 2;

  private static final boolean                STRING_COMPRESSION_ENABLED           = TCPropertiesImpl
                                                                                       .getProperties()
                                                                                       .getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_ENABLED);
  protected static final boolean              STRING_COMPRESSION_LOGGING_ENABLED   = TCPropertiesImpl
                                                                                       .getProperties()
                                                                                       .getBoolean(TCPropertiesConsts.L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_LOGGING_ENABLED);
  private static final int                    STRING_COMPRESSION_MIN_SIZE          = TCPropertiesImpl
                                                                                       .getProperties()
                                                                                       .getInt(TCPropertiesConsts.L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_MINSIZE);
  private static final ObjectStringSerializer NULL_SERIALIZER                      = new NullObjectStringSerializer();

  protected final ClassProvider               classProvider;

  public BaseDNAEncodingImpl(final ClassProvider classProvider) {
    this.classProvider = classProvider;
  }

  public void encode(Object value, final TCDataOutput output) {
    encode(value, output, NULL_SERIALIZER);
  }

  public void encode(Object value, final TCDataOutput output, final ObjectStringSerializer serializer) {
    if (value == null) {
      // Normally Null values should have already been converted to null ObjectID, but this is not true when there are
      // multiple versions of the same class in the cluster sharign data.
      value = ObjectID.NULL_ID;
    }

    final LiteralValues type = LiteralValues.valueFor(value);

    switch (type) {
      case ENUM:
        output.writeByte(TYPE_ID_ENUM);
        final Class enumClass = ((Enum) value).getDeclaringClass();
        writeString(enumClass.getName(), output, serializer);
        writeString(((Enum) value).name(), output, serializer);
        break;
      case ENUM_HOLDER:
        output.writeByte(TYPE_ID_ENUM_HOLDER);
        writeEnumInstance((EnumInstance) value, output, serializer);
        break;
      case JAVA_LANG_CLASS:
        output.writeByte(TYPE_ID_JAVA_LANG_CLASS);
        final Class c = (Class) value;
        writeString(c.getName(), output, serializer);
        break;
      case JAVA_LANG_CLASS_HOLDER:
        output.writeByte(TYPE_ID_JAVA_LANG_CLASS_HOLDER);
        writeClassInstance((ClassInstance) value, output, serializer);
        break;
      case BOOLEAN:
        output.writeByte(TYPE_ID_BOOLEAN);
        output.writeBoolean(((Boolean) value).booleanValue());
        break;
      case BYTE:
        output.writeByte(TYPE_ID_BYTE);
        output.writeByte(((Byte) value).byteValue());
        break;
      case CHARACTER:
        output.writeByte(TYPE_ID_CHAR);
        output.writeChar(((Character) value).charValue());
        break;
      case DOUBLE:
        output.writeByte(TYPE_ID_DOUBLE);
        output.writeDouble(((Double) value).doubleValue());
        break;
      case FLOAT:
        output.writeByte(TYPE_ID_FLOAT);
        output.writeFloat(((Float) value).floatValue());
        break;
      case INTEGER:
        output.writeByte(TYPE_ID_INT);
        output.writeInt(((Integer) value).intValue());
        break;
      case LONG:
        output.writeByte(TYPE_ID_LONG);
        output.writeLong(((Long) value).longValue());
        break;
      case SHORT:
        output.writeByte(TYPE_ID_SHORT);
        output.writeShort(((Short) value).shortValue());
        break;
      case STRING:
        final String s = (String) value;

        if (STRING_COMPRESSION_ENABLED && s.length() >= STRING_COMPRESSION_MIN_SIZE) {
          output.writeByte(TYPE_ID_STRING_COMPRESSED);
          writeCompressedString(s, output);
        } else {
          output.writeByte(TYPE_ID_STRING);
          writeString(s, output, serializer);
        }
        break;
      case STRING_BYTES:
        final UTF8ByteDataHolder utfBytes = (UTF8ByteDataHolder) value;

        output.writeByte(TYPE_ID_STRING_BYTES);
        serializer.writeStringBytes(output, utfBytes.getBytes());
        break;
      case STRING_BYTES_COMPRESSED:
        final UTF8ByteCompressedDataHolder utfCompressedBytes = (UTF8ByteCompressedDataHolder) value;

        output.writeByte(TYPE_ID_STRING_COMPRESSED);
        output.writeInt(utfCompressedBytes.getUncompressedStringLength());
        writeByteArray(utfCompressedBytes.getBytes(), output);
        output.writeInt(utfCompressedBytes.getStringLength());
        output.writeInt(utfCompressedBytes.getStringHash());
        break;

      case OBJECT_ID:
        output.writeByte(TYPE_ID_REFERENCE);
        output.writeLong(((ObjectID) value).toLong());
        break;
      case ARRAY:
        encodeArray(value, output);
        break;
      default:
        throw Assert.failure("Illegal type (" + type + "):" + value);
    }

    // unreachable
  }

  private void writeEnumInstance(final EnumInstance value, final TCDataOutput output, ObjectStringSerializer serializer) {
    writeStringBytes(value.getClassInstance().getName().getBytes(), output, serializer);
    writeStringBytes(((UTF8ByteDataHolder) value.getEnumName()).getBytes(), output, serializer);
  }

  private void writeClassInstance(final ClassInstance value, final TCDataOutput output,
                                  ObjectStringSerializer serializer) {
    writeStringBytes(value.getName().getBytes(), output, serializer);
  }

  private void writeString(final String string, final TCDataOutput output, final ObjectStringSerializer serializer) {
    try {
      writeStringBytes(string.getBytes("UTF-8"), output, serializer);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private void writeStringBytes(byte[] bytes, TCDataOutput output, ObjectStringSerializer serializer) {
    serializer.writeStringBytes(output, bytes);
  }

  private void writeCompressedString(final String string, final TCDataOutput output) {
    final byte[] uncompressed = StringCompressionUtil.stringToUncompressedBin(string);
    final CompressedData compressedInfo = StringCompressionUtil.compressBin(uncompressed);
    final byte[] compressed = compressedInfo.getCompressedData();
    final int compressedSize = compressedInfo.getCompressedSize();

    // XXX:: We are writing the original string's uncompressed byte[] length so that we save a couple of copies when
    // decompressing
    output.writeInt(uncompressed.length);
    writeByteArray(compressed, 0, compressedSize, output);

    // write string metadata so we can avoid decompression on later L1s
    output.writeInt(string.length());
    output.writeInt(string.hashCode());

    if (STRING_COMPRESSION_LOGGING_ENABLED) {
      logger.info("Compressed String of size : " + string.length() + " bytes : " + uncompressed.length
                  + " to  bytes : " + compressed.length);
    }
  }

  private void writeByteArray(final byte[] bytes, final int offset, final int length, final TCDataOutput output) {
    output.writeInt(length);
    output.write(bytes, offset, length);
  }

  private void writeByteArray(final byte bytes[], final TCDataOutput output) {
    output.writeInt(bytes.length);
    output.write(bytes);
  }

  /* This method is an optimized method for writing char array when no check is needed */
  // private void writeCharArray(char[] chars, TCDataOutput output) {
  // output.writeInt(chars.length);
  // for (int i = 0, n = chars.length; i < n; i++) {
  // output.writeChar(chars[i]);
  // }
  // }
  protected byte[] readByteArray(final TCDataInput input) throws IOException {
    final int length = input.readInt();
    if (length >= BYTE_WARN) {
      logger.warn("Attempting to allocate a large byte array of size: " + length);
    }
    final byte[] array = new byte[length];
    input.readFully(array);
    return array;
  }

  public Object decode(final TCDataInput input) throws IOException, ClassNotFoundException {
    return decode(input, NULL_SERIALIZER);
  }

  public Object decode(final TCDataInput input, final ObjectStringSerializer serializer) throws IOException,
      ClassNotFoundException {
    final byte type = input.readByte();

    switch (type) {
      case TYPE_ID_ENUM:
        return readEnum(input, type, serializer);
      case TYPE_ID_ENUM_HOLDER:
        return readEnum(input, type, serializer);
      case TYPE_ID_JAVA_LANG_CLASS:
        return readClass(input, type, serializer);
      case TYPE_ID_JAVA_LANG_CLASS_HOLDER:
        return readClass(input, type, serializer);
      case TYPE_ID_BOOLEAN:
        return Boolean.valueOf(input.readBoolean());
      case TYPE_ID_BYTE:
        return Byte.valueOf(input.readByte());
      case TYPE_ID_CHAR:
        return Character.valueOf(input.readChar());
      case TYPE_ID_DOUBLE:
        return Double.valueOf(input.readDouble());
      case TYPE_ID_FLOAT:
        return Float.valueOf(input.readFloat());
      case TYPE_ID_INT:
        return Integer.valueOf(input.readInt());
      case TYPE_ID_LONG:
        return Long.valueOf(input.readLong());
      case TYPE_ID_SHORT:
        return Short.valueOf(input.readShort());
      case TYPE_ID_STRING:
        return readString(input, type, serializer);
      case TYPE_ID_STRING_COMPRESSED:
        return readCompressedString(input);
      case TYPE_ID_STRING_BYTES:
        return readString(input, type, serializer);
      case TYPE_ID_REFERENCE:
        return new ObjectID(input.readLong());
      case TYPE_ID_ARRAY:
        return decodeArray(input);
      default:
        throw Assert.failure("Illegal type (" + type + ")");
    }

    // unreachable
  }

  // private char[] readCharArray(TCDataInput input) throws IOException {
  // int length = input.readInt();
  // if (length >= CHAR_WARN) {
  // logger.warn("Attempting to allocate a large char array of size: " + length);
  // }
  // char[] array = new char[length];
  // for (int i = 0, n = array.length; i < n; i++) {
  // array[i] = input.readChar();
  // }
  // return array;
  // }

  public void encodeArray(final Object value, final TCDataOutput output) {
    encodeArray(value, output, value == null ? -1 : Array.getLength(value));
  }

  public void encodeArray(final Object value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_ARRAY);

    if (value == null) {
      output.writeInt(-1);
      return;
    } else {
      output.writeInt(length);
    }

    final Class type = value.getClass().getComponentType();

    if (type.isPrimitive()) {
      output.writeByte(ARRAY_TYPE_PRIMITIVE);
      switch (primitiveClassMap.get(type)) {
        case TYPE_ID_BOOLEAN:
          encodeBooleanArray((boolean[]) value, output, length);
          break;
        case TYPE_ID_BYTE:
          encodeByteArray((byte[]) value, output, length);
          break;
        case TYPE_ID_CHAR:
          encodeCharArray((char[]) value, output, length);
          break;
        case TYPE_ID_SHORT:
          encodeShortArray((short[]) value, output, length);
          break;
        case TYPE_ID_INT:
          encodeIntArray((int[]) value, output, length);
          break;
        case TYPE_ID_LONG:
          encodeLongArray((long[]) value, output, length);
          break;
        case TYPE_ID_FLOAT:
          encodeFloatArray((float[]) value, output, length);
          break;
        case TYPE_ID_DOUBLE:
          encodeDoubleArray((double[]) value, output, length);
          break;
        default:
          throw Assert.failure("unknown primitive array type: " + type);
      }
    } else {
      output.writeByte(ARRAY_TYPE_NON_PRIMITIVE);
      encodeObjectArray((Object[]) value, output, length);
    }
  }

  private void encodeByteArray(final byte[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_BYTE);
    output.write(value, 0, length);

  }

  private void encodeObjectArray(final Object[] value, final TCDataOutput output, final int length) {
    for (int i = 0; i < length; i++) {
      encode(value[i], output);
    }
  }

  private void encodeDoubleArray(final double[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_DOUBLE);
    for (int i = 0; i < length; i++) {
      output.writeDouble(value[i]);
    }
  }

  private void encodeFloatArray(final float[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_FLOAT);
    for (int i = 0; i < length; i++) {
      output.writeFloat(value[i]);
    }
  }

  private void encodeLongArray(final long[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_LONG);
    for (int i = 0; i < length; i++) {
      output.writeLong(value[i]);
    }
  }

  private void encodeIntArray(final int[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_INT);
    for (int i = 0; i < length; i++) {
      output.writeInt(value[i]);
    }
  }

  private void encodeShortArray(final short[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_SHORT);
    for (int i = 0; i < length; i++) {
      output.writeShort(value[i]);
    }
  }

  private void encodeCharArray(final char[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_CHAR);
    for (int i = 0; i < length; i++) {
      output.writeChar(value[i]);
    }
  }

  private void encodeBooleanArray(final boolean[] value, final TCDataOutput output, final int length) {
    output.writeByte(TYPE_ID_BOOLEAN);
    for (int i = 0; i < length; i++) {
      output.writeBoolean(value[i]);
    }
  }

  private void checkSize(final Class type, final int threshold, final int len) {
    if (len >= threshold) {
      logger.warn("Attempt to read a " + type + " array of len: " + len + "; threshold=" + threshold);
    }
  }

  private Object decodeArray(final TCDataInput input) throws IOException, ClassNotFoundException {
    final int len = input.readInt();
    if (len < 0) { return null; }

    final byte arrayType = input.readByte();
    switch (arrayType) {
      case ARRAY_TYPE_PRIMITIVE:
        return decodePrimitiveArray(len, input);
      case ARRAY_TYPE_NON_PRIMITIVE:
        return decodeNonPrimitiveArray(len, input);
      default:
        throw Assert.failure("unknown array type: " + arrayType);
    }

    // unreachable
  }

  private Object[] decodeNonPrimitiveArray(final int len, final TCDataInput input) throws IOException,
      ClassNotFoundException {
    checkSize(Object.class, REF_WARN, len);
    final Object[] rv = new Object[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = decode(input);
    }

    return rv;
  }

  private Object decodePrimitiveArray(final int len, final TCDataInput input) throws IOException {
    final byte type = input.readByte();

    switch (type) {
      case TYPE_ID_BOOLEAN:
        checkSize(Boolean.TYPE, BOOLEAN_WARN, len);
        return decodeBooleanArray(len, input);
      case TYPE_ID_BYTE:
        checkSize(Byte.TYPE, BYTE_WARN, len);
        return decodeByteArray(len, input);
      case TYPE_ID_CHAR:
        checkSize(Character.TYPE, CHAR_WARN, len);
        return decodeCharArray(len, input);
      case TYPE_ID_DOUBLE:
        checkSize(Double.TYPE, DOUBLE_WARN, len);
        return decodeDoubleArray(len, input);
      case TYPE_ID_FLOAT:
        checkSize(Float.TYPE, FLOAT_WARN, len);
        return decodeFloatArray(len, input);
      case TYPE_ID_INT:
        checkSize(Integer.TYPE, INT_WARN, len);
        return decodeIntArray(len, input);
      case TYPE_ID_LONG:
        checkSize(Long.TYPE, LONG_WARN, len);
        return decodeLongArray(len, input);
      case TYPE_ID_SHORT:
        checkSize(Short.TYPE, SHORT_WARN, len);
        return decodeShortArray(len, input);
      default:
        throw Assert.failure("unknown prim type: " + type);
    }

    // unreachable
  }

  private short[] decodeShortArray(final int len, final TCDataInput input) throws IOException {
    final short[] rv = new short[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readShort();
    }
    return rv;
  }

  private long[] decodeLongArray(final int len, final TCDataInput input) throws IOException {
    final long[] rv = new long[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readLong();
    }
    return rv;
  }

  private int[] decodeIntArray(final int len, final TCDataInput input) throws IOException {
    final int[] rv = new int[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readInt();
    }
    return rv;
  }

  private float[] decodeFloatArray(final int len, final TCDataInput input) throws IOException {
    final float[] rv = new float[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readFloat();
    }
    return rv;
  }

  private double[] decodeDoubleArray(final int len, final TCDataInput input) throws IOException {
    final double[] rv = new double[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readDouble();
    }
    return rv;
  }

  private char[] decodeCharArray(final int len, final TCDataInput input) throws IOException {
    final char[] rv = new char[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readChar();
    }
    return rv;
  }

  private byte[] decodeByteArray(final int len, final TCDataInput input) throws IOException {
    final byte[] rv = new byte[len];
    if (len != 0) {
      final int read = input.read(rv, 0, len);
      if (read != len) { throw new IOException("read " + read + " bytes, expected " + len); }
    }
    return rv;
  }

  private boolean[] decodeBooleanArray(final int len, final TCDataInput input) throws IOException {
    final boolean[] rv = new boolean[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readBoolean();
    }
    return rv;
  }

  private Object readEnum(final TCDataInput input, final byte type, ObjectStringSerializer serializer)
      throws IOException, ClassNotFoundException {
    final UTF8ByteDataHolder name = new UTF8ByteDataHolder(readStringBytes(input, serializer));
    final byte[] data = readStringBytes(input, serializer);

    if (useStringEnumRead(type)) {
      final Class enumType = new ClassInstance(name).asClass(this.classProvider);
      final String enumName = new String(data, "UTF-8");
      return Enum.valueOf(enumType, enumName);
    } else {
      final ClassInstance clazzInstance = new ClassInstance(name);
      final UTF8ByteDataHolder enumName = new UTF8ByteDataHolder(data);
      return new EnumInstance(clazzInstance, enumName);
    }
  }

  protected abstract boolean useStringEnumRead(byte type);

  protected abstract boolean useClassProvider(byte type, byte typeToCheck);

  private Object readClass(final TCDataInput input, final byte type, ObjectStringSerializer serializer)
      throws IOException, ClassNotFoundException {
    final UTF8ByteDataHolder name = new UTF8ByteDataHolder(readStringBytes(input, serializer));

    if (useClassProvider(type, TYPE_ID_JAVA_LANG_CLASS)) {
      return new ClassInstance(name).asClass(this.classProvider);
    } else {
      return new ClassInstance(name);
    }
  }

  private Object readString(final TCDataInput input, final byte type, ObjectStringSerializer serializer)
      throws IOException {
    final byte[] data = readStringBytes(input, serializer);
    if (useUTF8String(type)) {
      // special case the empty string to save memory
      if (data.length == 0) { return ""; }

      return new String(data, "UTF-8");
    } else {
      return new UTF8ByteDataHolder(data);
    }
  }

  private byte[] readStringBytes(TCDataInput input, ObjectStringSerializer serializer) throws IOException {
    return serializer.readStringBytes(input);
  }

  protected abstract boolean useUTF8String(byte type);

  protected Object readCompressedString(final TCDataInput input) throws IOException {
    final int stringUncompressedByteLength = input.readInt();
    final byte[] data = readByteArray(input);

    final int stringLength = input.readInt();
    final int stringHash = input.readInt();

    return new UTF8ByteCompressedDataHolder(data, stringUncompressedByteLength, stringLength, stringHash);
  }

  public static String inflateCompressedString(final byte[] data, int length) {
    try {
      final ByteArrayInputStream bais = new ByteArrayInputStream(data);
      final InflaterInputStream iis = new InflaterInputStream(bais);
      final byte uncompressed[] = new byte[length];
      int read;
      int offset = 0;
      while (length > 0 && (read = iis.read(uncompressed, offset, length)) != -1) {
        offset += read;
        length -= read;
      }
      iis.close();
      Assert.assertEquals(0, length);
      return new String(uncompressed, "UTF-8");
    } catch (final IOException e) {
      throw new AssertionError(e);
    }
  }

  private static final TObjectIntHashMap primitiveClassMap = new TObjectIntHashMap();

  static {
    primitiveClassMap.put(java.lang.Boolean.TYPE, TYPE_ID_BOOLEAN);
    primitiveClassMap.put(java.lang.Byte.TYPE, TYPE_ID_BYTE);
    primitiveClassMap.put(java.lang.Character.TYPE, TYPE_ID_CHAR);
    primitiveClassMap.put(java.lang.Double.TYPE, TYPE_ID_DOUBLE);
    primitiveClassMap.put(java.lang.Float.TYPE, TYPE_ID_FLOAT);
    primitiveClassMap.put(java.lang.Integer.TYPE, TYPE_ID_INT);
    primitiveClassMap.put(java.lang.Long.TYPE, TYPE_ID_LONG);
    primitiveClassMap.put(java.lang.Short.TYPE, TYPE_ID_SHORT);
  }

}
