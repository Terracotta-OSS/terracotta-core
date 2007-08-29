/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.exception.TCRuntimeException;
import com.tc.io.TCByteArrayOutputStream;
import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import gnu.trove.TObjectIntHashMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Currency;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Utility for encoding/decoding DNA
 */
public class DNAEncodingImpl implements DNAEncoding {

  // XXX: These warning thresholds should be done in a non-static way so they can be made configurable
  // and architecture sensitive.
  private static final int           WARN_THRESHOLD                       = 8 * 1000 * 1000;
  private static final int           BOOLEAN_WARN                         = WARN_THRESHOLD / 1;
  private static final int           BYTE_WARN                            = WARN_THRESHOLD / 1;
  private static final int           CHAR_WARN                            = WARN_THRESHOLD / 2;
  private static final int           DOUBLE_WARN                          = WARN_THRESHOLD / 8;
  private static final int           FLOAT_WARN                           = WARN_THRESHOLD / 4;
  private static final int           INT_WARN                             = WARN_THRESHOLD / 4;
  private static final int           LONG_WARN                            = WARN_THRESHOLD / 8;
  private static final int           SHORT_WARN                           = WARN_THRESHOLD / 2;
  private static final int           REF_WARN                             = WARN_THRESHOLD / 4;

  static final byte                  LOGICAL_ACTION_TYPE                  = 1;
  static final byte                  PHYSICAL_ACTION_TYPE                 = 2;
  static final byte                  ARRAY_ELEMENT_ACTION_TYPE            = 3;
  static final byte                  ENTIRE_ARRAY_ACTION_TYPE             = 4;
  static final byte                  LITERAL_VALUE_ACTION_TYPE            = 5;
  static final byte                  PHYSICAL_ACTION_TYPE_REF_OBJECT      = 6;
  static final byte                  SUB_ARRAY_ACTION_TYPE                = 7;

  private static final LiteralValues literalValues                        = new LiteralValues();
  private static final TCLogger      logger                               = TCLogging.getLogger(DNAEncodingImpl.class);

  private static final byte          TYPE_ID_REFERENCE                    = 1;
  private static final byte          TYPE_ID_BOOLEAN                      = 2;
  private static final byte          TYPE_ID_BYTE                         = 3;
  private static final byte          TYPE_ID_CHAR                         = 4;
  private static final byte          TYPE_ID_DOUBLE                       = 5;
  private static final byte          TYPE_ID_FLOAT                        = 6;
  private static final byte          TYPE_ID_INT                          = 7;
  private static final byte          TYPE_ID_LONG                         = 10;
  private static final byte          TYPE_ID_SHORT                        = 11;
  private static final byte          TYPE_ID_STRING                       = 12;
  private static final byte          TYPE_ID_STRING_BYTES                 = 13;
  private static final byte          TYPE_ID_ARRAY                        = 14;
  private static final byte          TYPE_ID_JAVA_LANG_CLASS              = 15;
  private static final byte          TYPE_ID_JAVA_LANG_CLASS_HOLDER       = 16;
  private static final byte          TYPE_ID_BIG_INTEGER                  = 17;
  private static final byte          TYPE_ID_STACK_TRACE_ELEMENT          = 18;
  private static final byte          TYPE_ID_BIG_DECIMAL                  = 19;
  private static final byte          TYPE_ID_JAVA_LANG_CLASSLOADER        = 20;
  private static final byte          TYPE_ID_JAVA_LANG_CLASSLOADER_HOLDER = 21;
  private static final byte          TYPE_ID_ENUM                         = 22;
  private static final byte          TYPE_ID_ENUM_HOLDER                  = 23;
  private static final byte          TYPE_ID_CURRENCY                     = 24;
  private static final byte          TYPE_ID_STRING_COMPRESSED            = 25;

  private static final byte          ARRAY_TYPE_PRIMITIVE                 = 1;
  private static final byte          ARRAY_TYPE_NON_PRIMITIVE             = 2;

  private final ClassProvider        classProvider;
  private final byte                 policy;

  private static final ClassProvider FAILURE_PROVIDER                     = new FailureClassProvider();
  private static final ClassProvider LOCAL_PROVIDER                       = new LocalClassProvider();

  private static final boolean       STRING_COMPRESSION_ENABLED           = TCPropertiesImpl
                                                                              .getProperties()
                                                                              .getBoolean(
                                                                                          "l1.transactionmanager.strings.compress.enabled");
  private static final boolean       STRING_COMPRESSION_LOGGING_ENABLED   = TCPropertiesImpl
                                                                              .getProperties()
                                                                              .getBoolean(
                                                                                          "l1.transactionmanager.strings.compress.logging.enabled");
  private static final int           STRING_COMPRESSION_MIN_SIZE          = TCPropertiesImpl
                                                                              .getProperties()
                                                                              .getInt(
                                                                                      "l1.transactionmanager.strings.compress.minSize");

  /**
   * Used in the Applicators. The policy is set to APPLICATOR.
   */
  public DNAEncodingImpl(ClassProvider classProvider) {
    this.classProvider = classProvider;
    this.policy = APPLICATOR;
  }

  public DNAEncodingImpl(byte policy) {
    this.policy = policy;
    // you only want this version on the server where you won't be expanding java.lang.Class instances
    if (policy == STORAGE) {
      this.classProvider = FAILURE_PROVIDER;
    } else if (policy == SERIALIZER) {
      this.classProvider = LOCAL_PROVIDER;
    } else {
      throw new AssertionError("Policy not valid : " + policy + " : For APPLICATORS use the other contructor !");
    }
  }

  public byte getPolicy() {
    return this.policy;
  }

  public void encodeClassLoader(Object value, TCDataOutput output) {
    output.writeByte(TYPE_ID_JAVA_LANG_CLASSLOADER);
    writeString(classProvider.getLoaderDescriptionFor((ClassLoader) value), output);
  }

  /**
   * The reason that we use reflection here is that Enum is a jdk 1.5 construct and this project is jdk 1.4 compliance.
   */
  private Object getEnumName(Object enumObject) {
    try {
      Method m = enumObject.getClass().getMethod("name", new Class[0]);
      m.setAccessible(true);
      Object val;
      val = m.invoke(enumObject, new Object[0]);
      return val;
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void encode(Object value, TCDataOutput output) {
    if (value == null) {
      // Normally Null values should have already been converted to null ObjectID, but this is not true when there are
      // multiple versions of the same class in the cluster sharign data.
      value = ObjectID.NULL_ID;
    }

    // final Class valueClass = value.getClass();
    // final int type = literalValues.valueFor(valueClass.getName());
    final int type = literalValues.valueFor(value);

    switch (type) {
      case LiteralValues.CURRENCY:
        output.writeByte(TYPE_ID_CURRENCY);
        writeString(((Currency) value).getCurrencyCode(), output);
        break;
      case LiteralValues.ENUM:
        output.writeByte(TYPE_ID_ENUM);
        Class enumClass = getEnumDeclaringClass(value);
        writeString(enumClass.getName(), output);
        writeString(classProvider.getLoaderDescriptionFor(enumClass), output);

        Object name = getEnumName(value);
        writeString((String) name, output);
        break;
      case LiteralValues.ENUM_HOLDER:
        output.writeByte(TYPE_ID_ENUM_HOLDER);
        writeEnumInstance((EnumInstance) value, output);
        break;
      case LiteralValues.JAVA_LANG_CLASSLOADER:
        encodeClassLoader(value, output);
        break;
      case LiteralValues.JAVA_LANG_CLASSLOADER_HOLDER:
        output.writeByte(TYPE_ID_JAVA_LANG_CLASSLOADER_HOLDER);
        writeClassLoaderInstance((ClassLoaderInstance) value, output);
        break;
      case LiteralValues.JAVA_LANG_CLASS:
        output.writeByte(TYPE_ID_JAVA_LANG_CLASS);
        Class c = (Class) value;
        writeString(c.getName(), output);
        writeString(classProvider.getLoaderDescriptionFor(c), output);
        break;
      case LiteralValues.JAVA_LANG_CLASS_HOLDER:
        output.writeByte(TYPE_ID_JAVA_LANG_CLASS_HOLDER);
        writeClassInstance((ClassInstance) value, output);
        break;
      case LiteralValues.BOOLEAN:
        output.writeByte(TYPE_ID_BOOLEAN);
        output.writeBoolean(((Boolean) value).booleanValue());
        break;
      case LiteralValues.BYTE:
        output.writeByte(TYPE_ID_BYTE);
        output.writeByte(((Byte) value).byteValue());
        break;
      case LiteralValues.CHARACTER:
        output.writeByte(TYPE_ID_CHAR);
        output.writeChar(((Character) value).charValue());
        break;
      case LiteralValues.DOUBLE:
        output.writeByte(TYPE_ID_DOUBLE);
        output.writeDouble(((Double) value).doubleValue());
        break;
      case LiteralValues.FLOAT:
        output.writeByte(TYPE_ID_FLOAT);
        output.writeFloat(((Float) value).floatValue());
        break;
      case LiteralValues.INTEGER:
        output.writeByte(TYPE_ID_INT);
        output.writeInt(((Integer) value).intValue());
        break;
      case LiteralValues.LONG:
        output.writeByte(TYPE_ID_LONG);
        output.writeLong(((Long) value).longValue());
        break;
      case LiteralValues.SHORT:
        output.writeByte(TYPE_ID_SHORT);
        output.writeShort(((Short) value).shortValue());
        break;
      case LiteralValues.STRING:
        String s = (String) value;
        if (STRING_COMPRESSION_ENABLED && s.length() >= STRING_COMPRESSION_MIN_SIZE) {
          output.writeByte(TYPE_ID_STRING_COMPRESSED);
          writeCompressedString(s, output);
        } else {
          output.writeByte(TYPE_ID_STRING);
          writeString(s, output);
        }
        break;
      case LiteralValues.STRING_BYTES:
        UTF8ByteDataHolder utfBytes = (UTF8ByteDataHolder) value;
        if (utfBytes.isCompressed()) {
          output.writeByte(TYPE_ID_STRING_COMPRESSED);
          output.writeInt(utfBytes.getUnCompressedStringLength());
          writeByteArray(utfBytes.getBytes(), output);
        } else {
          output.writeByte(TYPE_ID_STRING_BYTES);
          writeByteArray(utfBytes.getBytes(), output);
        }
        break;
      case LiteralValues.OBJECT_ID:
        output.writeByte(TYPE_ID_REFERENCE);
        output.writeLong(((ObjectID) value).toLong());
        break;
      case LiteralValues.STACK_TRACE_ELEMENT:
        output.writeByte(TYPE_ID_STACK_TRACE_ELEMENT);
        StackTraceElement ste = (StackTraceElement) value;
        writeStackTraceElement(ste, output);
        break;
      case LiteralValues.BIG_INTEGER:
        output.writeByte(TYPE_ID_BIG_INTEGER);
        writeByteArray(((BigInteger) value).toByteArray(), output);
        break;
      case LiteralValues.BIG_DECIMAL:
        output.writeByte(TYPE_ID_BIG_DECIMAL);
        writeByteArray(((BigDecimal) value).toString().getBytes(), output);
        break;
      case LiteralValues.ARRAY:
        encodeArray(value, output);
        break;
      default:
        throw Assert.failure("Illegal type (" + type + "):" + value);
    }

    // unreachable
  }

  private void writeStackTraceElement(StackTraceElement ste, TCDataOutput output) {
    output.writeString(ste.getClassName());
    output.writeString(ste.getMethodName());
    output.writeString(ste.getFileName());
    output.writeInt(ste.getLineNumber());
  }

  private void writeEnumInstance(EnumInstance value, TCDataOutput output) {
    writeByteArray(value.getClassInstance().getName().getBytes(), output);
    writeByteArray(value.getClassInstance().getLoaderDef().getBytes(), output);
    writeByteArray(((UTF8ByteDataHolder) value.getEnumName()).getBytes(), output);
  }

  private void writeClassLoaderInstance(ClassLoaderInstance value, TCDataOutput output) {
    writeByteArray(value.getLoaderDef().getBytes(), output);
  }

  private void writeClassInstance(ClassInstance value, TCDataOutput output) {
    writeByteArray(value.getName().getBytes(), output);
    writeByteArray(value.getLoaderDef().getBytes(), output);
  }

  private void writeString(String string, TCDataOutput output) {
    try {
      writeByteArray(string.getBytes("UTF-8"), output);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private void writeCompressedString(String string, TCDataOutput output) {
    try {
      TCByteArrayOutputStream byteArrayOS = new TCByteArrayOutputStream(4096);
      // Stride is 512 bytes by default, should I increase ?
      DeflaterOutputStream dos = new DeflaterOutputStream(byteArrayOS);
      byte[] uncompressed = string.getBytes("UTF-8");
      dos.write(uncompressed);
      dos.close();
      byte[] compressed = byteArrayOS.getInternalArray();
      // XXX:: We are writting the original string's length so that we save a couple of copies when decompressing
      output.writeInt(uncompressed.length);
      writeByteArray(compressed, 0, byteArrayOS.size(), output);
      if (STRING_COMPRESSION_LOGGING_ENABLED) {
        logger.info("Compressed String of size : " + string.length() + " bytes : " + uncompressed.length
                    + " to  bytes : " + compressed.length);
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void writeByteArray(byte[] bytes, int offset, int length, TCDataOutput output) {
    output.writeInt(length);
    output.write(bytes, offset, length);
  }

  private void writeByteArray(byte bytes[], TCDataOutput output) {
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
  private byte[] readByteArray(TCDataInput input) throws IOException {
    int length = input.readInt();
    if (length >= BYTE_WARN) {
      logger.warn("Attempting to allocate a large byte array of size: " + length);
    }
    byte[] array = new byte[length];
    input.readFully(array);
    return array;
  }

  public Object decode(TCDataInput input) throws IOException, ClassNotFoundException {
    final byte type = input.readByte();

    switch (type) {
      case TYPE_ID_CURRENCY:
        return readCurrency(input, type);
      case TYPE_ID_ENUM:
        return readEnum(input, type);
      case TYPE_ID_ENUM_HOLDER:
        return readEnum(input, type);
      case TYPE_ID_JAVA_LANG_CLASSLOADER:
        return readClassLoader(input, type);
      case TYPE_ID_JAVA_LANG_CLASSLOADER_HOLDER:
        return readClassLoader(input, type);
      case TYPE_ID_JAVA_LANG_CLASS:
        return readClass(input, type);
      case TYPE_ID_JAVA_LANG_CLASS_HOLDER:
        return readClass(input, type);
      case TYPE_ID_BOOLEAN:
        return new Boolean(input.readBoolean());
      case TYPE_ID_BYTE:
        return new Byte(input.readByte());
      case TYPE_ID_CHAR:
        return new Character(input.readChar());
      case TYPE_ID_DOUBLE:
        return new Double(input.readDouble());
      case TYPE_ID_FLOAT:
        return new Float(input.readFloat());
      case TYPE_ID_INT:
        return new Integer(input.readInt());
      case TYPE_ID_LONG:
        return new Long(input.readLong());
      case TYPE_ID_SHORT:
        return new Short(input.readShort());
      case TYPE_ID_STRING:
        return readString(input, type);
      case TYPE_ID_STRING_COMPRESSED:
        return readCompressedString(input);
      case TYPE_ID_STRING_BYTES:
        return readString(input, type);
      case TYPE_ID_REFERENCE:
        return new ObjectID(input.readLong());
      case TYPE_ID_ARRAY:
        return decodeArray(input);
      case TYPE_ID_STACK_TRACE_ELEMENT:
        return readStackTraceElement(input);
      case TYPE_ID_BIG_INTEGER:
        byte[] b1 = readByteArray(input);
        return new BigInteger(b1);
      case TYPE_ID_BIG_DECIMAL:
        // char[] chars = readCharArray(input); // Unfortunately this is 1.5 specific
        byte[] b2 = readByteArray(input);
        return new BigDecimal(new String(b2));
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

  private Object readStackTraceElement(TCDataInput input) throws IOException, ClassNotFoundException {
    String className = input.readString();
    String methodName = input.readString();
    String fileName = input.readString();
    int lineNumber = input.readInt();
    return createStackTraceElement(className, fileName, methodName, lineNumber);
  }

  /*
   * This method uses reflection as 1.4 doesnt have a public constructor for stack trace element and 1.5 removed the
   * private no arg constructor. XXX::This is an ugly hack that I would like to getaway from
   */
  private Object createStackTraceElement(String className, String fileName, String methodName, int lineNumber)
      throws ClassNotFoundException, IOException {
    Class clazz = Class.forName("java.lang.StackTraceElement");
    Constructor constructors[] = clazz.getDeclaredConstructors();
    for (int i = 0; i < constructors.length; i++) {
      Class[] types = constructors[i].getParameterTypes();
      if (types.length == 0) {
        // This is 1.4
        return createStackTraceElementJDK14(clazz, constructors[i], className, fileName, methodName, lineNumber);
      } else if (types.length == 4 && types[0] == String.class && types[1] == String.class && types[2] == String.class
                 && types[3] == int.class) {
        // This is 1.5
        return createStackTraceElementJDK15(clazz, constructors[i], className, fileName, methodName, lineNumber);
      }
    }
    throw new ClassNotFoundException("java.lang.StackTraceElement : Both known constructors not found !");
  }

  private Object createStackTraceElementJDK14(Class clazz, Constructor constructor, String className, String fileName,
                                              String methodName, int lineNumber) throws IOException {
    try {
      constructor.setAccessible(true);
      Object i = constructor.newInstance(new Object[0]);
      Field[] fields = clazz.getDeclaredFields();
      byte set = 0x00;
      for (int j = 0; j < fields.length; j++) {
        fields[j].setAccessible(true);
        if ("declaringClass".equalsIgnoreCase(fields[j].getName())) {
          fields[j].set(i, className);
          set |= 0x01;
        } else if ("methodName".equalsIgnoreCase(fields[j].getName())) {
          fields[j].set(i, methodName);
          set |= 0x02;
        } else if ("fileName".equalsIgnoreCase(fields[j].getName())) {
          fields[j].set(i, fileName);
          set |= 0x04;
        } else if ("lineNumber".equalsIgnoreCase(fields[j].getName())) {
          fields[j].setInt(i, lineNumber);
          set |= 0x08;
        }
      }
      Assert.assertTrue(set == 0x0F);
      return i;
    } catch (Exception ex) {
      IOException ioe = new IOException();
      ioe.initCause(ex);
      throw ioe;
    }
  }

  private Object createStackTraceElementJDK15(Class clazz, Constructor constructor, String className, String fileName,
                                              String methodName, int lineNumber) throws IOException {
    try {
      Object params[] = new Object[4];
      params[0] = className;
      params[1] = methodName;
      params[2] = fileName;
      params[3] = new Integer(lineNumber);
      return constructor.newInstance(params);
    } catch (Exception ex) {
      IOException ioe = new IOException();
      ioe.initCause(ex);
      throw ioe;
    }
  }

  public void encodeArray(Object value, TCDataOutput output) {
    encodeArray(value, output, value == null ? -1 : Array.getLength(value));
  }

  public void encodeArray(Object value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_ARRAY);

    if (value == null) {
      output.writeInt(-1);
      return;
    } else {
      output.writeInt(length);
    }

    Class type = value.getClass().getComponentType();

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

  private void encodeByteArray(byte[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_BYTE);

    for (int i = 0; i < length; i++) {
      output.write(value[i]);
    }
  }

  private void encodeObjectArray(Object[] value, TCDataOutput output, int length) {
    for (int i = 0; i < length; i++) {
      encode(value[i], output);
    }
  }

  private void encodeDoubleArray(double[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_DOUBLE);
    for (int i = 0; i < length; i++) {
      output.writeDouble(value[i]);
    }
  }

  private void encodeFloatArray(float[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_FLOAT);
    for (int i = 0; i < length; i++) {
      output.writeFloat(value[i]);
    }
  }

  private void encodeLongArray(long[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_LONG);
    for (int i = 0; i < length; i++) {
      output.writeLong(value[i]);
    }
  }

  private void encodeIntArray(int[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_INT);
    for (int i = 0; i < length; i++) {
      output.writeInt(value[i]);
    }
  }

  private void encodeShortArray(short[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_SHORT);
    for (int i = 0; i < length; i++) {
      output.writeShort(value[i]);
    }
  }

  private void encodeCharArray(char[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_CHAR);
    for (int i = 0; i < length; i++) {
      output.writeChar(value[i]);
    }
  }

  private void encodeBooleanArray(boolean[] value, TCDataOutput output, int length) {
    output.writeByte(TYPE_ID_BOOLEAN);
    for (int i = 0; i < length; i++) {
      output.writeBoolean(value[i]);
    }
  }

  private void checkSize(Class type, int threshold, int len) {
    if (len >= threshold) {
      logger.warn("Attempt to read a " + type + " array of len: " + len + "; threshold=" + threshold);
    }
  }

  private Object decodeArray(TCDataInput input) throws IOException, ClassNotFoundException {
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

  private Object[] decodeNonPrimitiveArray(int len, TCDataInput input) throws IOException, ClassNotFoundException {
    checkSize(Object.class, REF_WARN, len);
    Object[] rv = new Object[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = decode(input);
    }

    return rv;
  }

  private Object decodePrimitiveArray(int len, TCDataInput input) throws IOException {
    byte type = input.readByte();

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

  private short[] decodeShortArray(int len, TCDataInput input) throws IOException {
    short[] rv = new short[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readShort();
    }
    return rv;
  }

  private long[] decodeLongArray(int len, TCDataInput input) throws IOException {
    long[] rv = new long[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readLong();
    }
    return rv;
  }

  private int[] decodeIntArray(int len, TCDataInput input) throws IOException {
    int[] rv = new int[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readInt();
    }
    return rv;
  }

  private float[] decodeFloatArray(int len, TCDataInput input) throws IOException {
    float[] rv = new float[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readFloat();
    }
    return rv;
  }

  private double[] decodeDoubleArray(int len, TCDataInput input) throws IOException {
    double[] rv = new double[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readDouble();
    }
    return rv;
  }

  private char[] decodeCharArray(int len, TCDataInput input) throws IOException {
    char[] rv = new char[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readChar();
    }
    return rv;
  }

  private byte[] decodeByteArray(int len, TCDataInput input) throws IOException {
    byte[] rv = new byte[len];
    if (len != 0) {
      int read = input.read(rv, 0, len);
      if (read != len) { throw new IOException("read " + read + " bytes, expected " + len); }
    }
    return rv;
  }

  private boolean[] decodeBooleanArray(int len, TCDataInput input) throws IOException {
    boolean[] rv = new boolean[len];
    for (int i = 0, n = rv.length; i < n; i++) {
      rv[i] = input.readBoolean();
    }
    return rv;
  }

  /**
   * The reason that we use reflection here is because Enum is a jdk 1.5 construct and this project is jdk 1.4
   * compliance.
   */
  private Class getEnumDeclaringClass(Object enumObj) {
    try {
      Method m = enumObj.getClass().getMethod("getDeclaringClass", new Class[0]);
      Object enumDeclaringClass = m.invoke(enumObj, new Object[0]);
      return (Class) enumDeclaringClass;
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    }
  }

  /**
   * The reason that we use reflection here is because Enum is a jdk 1.5 construct and this project is jdk 1.4
   * compliance.
   */
  private Object enumValueOf(Class enumType, String enumName) {
    try {
      Method m = enumType.getMethod("valueOf", new Class[] { Class.class, String.class });
      Object enumObj = m.invoke(null, new Object[] { enumType, enumName });
      return enumObj;
    } catch (SecurityException e) {
      throw new TCRuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    }
  }

  private Object readCurrency(TCDataInput input, byte type) throws IOException {
    byte[] data = readByteArray(input);
    String currencyCode = new String(data, "UTF-8");
    return Currency.getInstance(currencyCode);
  }

  private Object readEnum(TCDataInput input, byte type) throws IOException, ClassNotFoundException {
    UTF8ByteDataHolder name = new UTF8ByteDataHolder(readByteArray(input));
    UTF8ByteDataHolder def = new UTF8ByteDataHolder(readByteArray(input));
    byte[] data = readByteArray(input);

    if ((policy == SERIALIZER && type == TYPE_ID_ENUM) || policy == APPLICATOR) {
      Class enumType = new ClassInstance(name, def).asClass(classProvider);

      String enumName = new String(data, "UTF-8");
      return enumValueOf(enumType, enumName);
    } else {
      ClassInstance clazzInstance = new ClassInstance(name, def);
      UTF8ByteDataHolder enumName = new UTF8ByteDataHolder(data);
      return new EnumInstance(clazzInstance, enumName);
    }
  }

  private Object readClassLoader(TCDataInput input, byte type) throws IOException {
    UTF8ByteDataHolder def = new UTF8ByteDataHolder(readByteArray(input));

    if ((policy == SERIALIZER && type == TYPE_ID_JAVA_LANG_CLASSLOADER) || policy == APPLICATOR) {
      return new ClassLoaderInstance(def).asClassLoader(classProvider);
    } else {
      return new ClassLoaderInstance(def);
    }
  }

  private Object readClass(TCDataInput input, byte type) throws IOException, ClassNotFoundException {
    UTF8ByteDataHolder name = new UTF8ByteDataHolder(readByteArray(input));
    UTF8ByteDataHolder def = new UTF8ByteDataHolder(readByteArray(input));

    if ((policy == SERIALIZER && type == TYPE_ID_JAVA_LANG_CLASS) || policy == APPLICATOR) {
      return new ClassInstance(name, def).asClass(classProvider);
    } else {
      return new ClassInstance(name, def);
    }
  }

  private Object readString(TCDataInput input, byte type) throws IOException {
    byte[] data = readByteArray(input);
    if ((policy == SERIALIZER && type == TYPE_ID_STRING) || policy == APPLICATOR) {
      return new String(data, "UTF-8");
    } else {
      return new UTF8ByteDataHolder(data);
    }
  }

  private Object readCompressedString(TCDataInput input) throws IOException {
    int stringLength = input.readInt();
    byte[] data = readByteArray(input);
    if (policy == APPLICATOR) {
      return inflateCompressedString(data, stringLength);
    } else {
      UTF8ByteDataHolder utfBytes = new UTF8ByteDataHolder(data, stringLength);
      return utfBytes;
    }
  }

  public static String inflateCompressedString(byte[] data, int length) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      InflaterInputStream iis = new InflaterInputStream(bais);
      byte uncompressed[] = new byte[length];
      int read;
      int offset = 0;
      while (length > 0 && (read = iis.read(uncompressed, offset, length)) != -1) {
        offset += read;
        length -= read;
      }
      iis.close();
      Assert.assertEquals(0, length);
      return new String(uncompressed, "UTF-8");
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private static class FailureClassProvider implements ClassProvider {

    public Class getClassFor(String className, String loaderDesc) {
      throw new AssertionError();
    }

    public String getLoaderDescriptionFor(Class clazz) {
      throw new AssertionError();
    }

    public ClassLoader getClassLoader(String loaderDesc) {
      throw new AssertionError();
    }

    public String getLoaderDescriptionFor(ClassLoader loader) {
      throw new AssertionError();
    }

    public void registerNamedLoader(NamedClassLoader loader) {
      throw new AssertionError();
    }
  }

  private static class LocalClassProvider implements ClassProvider {

    private static final String LOADER_ID = LocalClassProvider.class.getName() + "::CLASSPROVIDER";

    // This method assumes the Class is visible in this VM and can be loaded by the same class loader as this
    // object. Only used in SERIALIZER policy
    public Class getClassFor(String className, String loaderDesc) {
      Assert.assertEquals(LOADER_ID, loaderDesc);
      try {
        return Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }

    public String getLoaderDescriptionFor(Class clazz) {
      return LOADER_ID;
    }

    public ClassLoader getClassLoader(String loaderDesc) {
      Assert.assertEquals(LOADER_ID, loaderDesc);
      return ClassLoader.getSystemClassLoader();
    }

    public String getLoaderDescriptionFor(ClassLoader loader) {
      return LOADER_ID;
    }

    public void registerNamedLoader(NamedClassLoader loader) {
      // do nothing
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
