/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public abstract class AbstractNVPair implements NVPair {

  private static final AbstractNVPair TEMPLATE  = new Template();
  private static final ValueType[]    ALL_TYPES = ValueType.values();

  private final String                name;

  AbstractNVPair(String name) {
    this.name = name;
  }

  public final String getName() {
    return name;
  }

  @Override
  public final String toString() {
    return getType() + "(" + getName() + "," + valueAsString() + ")";
  }

  public abstract NVPair cloneWithNewName(String newName);

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof AbstractNVPair) {
      NVPair other = (NVPair) obj;
      if (other.getName().equals(getName())) { return basicEquals(other); }
    }
    return false;
  }

  abstract boolean basicEquals(NVPair other);

  @Override
  public final int hashCode() {
    return getType().hashCode() ^ name.hashCode() ^ valueAsString().hashCode();
  }

  // XXX: make this non-public when possible
  public abstract String valueAsString();

  public Object deserializeFrom(TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
    String readName = serializer.readString(in);
    byte ordinal = in.readByte();
    ValueType type = ALL_TYPES[ordinal];
    return type.deserializeFrom(readName, in, serializer);
  }

  public void serializeTo(TCByteBufferOutput out, ObjectStringSerializer serializer) {
    serializer.writeString(out, getName());

    ValueType type = getType();

    out.writeByte(type.ordinal());
    type.serializeTo(this, out, serializer);
  }

  public abstract ValueType getType();

  public static AbstractNVPair deserializeInstance(TCByteBufferInput in, ObjectStringSerializer serializer)
      throws IOException {
    return (AbstractNVPair) TEMPLATE.deserializeFrom(in, serializer);
  }

  private static class Template extends AbstractNVPair {

    Template() {
      super("");
    }

    @Override
    public String valueAsString() {
      throw new AssertionError();
    }

    @Override
    public ValueType getType() {
      throw new AssertionError();
    }

    @Override
    boolean basicEquals(NVPair other) {
      throw new AssertionError();
    }

    public Object getObjectValue() {
      throw new AssertionError();
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      throw new AssertionError();
    }

    public NVPair cloneWithNewValue(Object newValue) {
      throw new AssertionError();
    }
  }

  public static class ByteNVPair extends AbstractNVPair {
    private final byte value;

    public ByteNVPair(String name, byte value) {
      super(name);
      this.value = value;
    }

    public byte getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public ValueType getType() {
      return ValueType.BYTE;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof ByteNVPair) { return value == ((ByteNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new ByteNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new ByteNVPair(getName(), (Byte) newValue);
    }
  }

  public static class BooleanNVPair extends AbstractNVPair {
    private final boolean value;

    public BooleanNVPair(String name, boolean value) {
      super(name);
      this.value = value;
    }

    public boolean getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public ValueType getType() {
      return ValueType.BOOLEAN;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof BooleanNVPair) { return value == ((BooleanNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new BooleanNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new BooleanNVPair(getName(), (Boolean) newValue);
    }
  }

  public static class CharNVPair extends AbstractNVPair {
    private final char value;

    public CharNVPair(String name, char value) {
      super(name);
      this.value = value;
    }

    public char getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.CHAR;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof CharNVPair) { return value == ((CharNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new CharNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new CharNVPair(getName(), (Character) newValue);
    }
  }

  public static class DoubleNVPair extends AbstractNVPair {
    private final double value;

    public DoubleNVPair(String name, double value) {
      super(name);
      this.value = value;
    }

    public double getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.DOUBLE;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof DoubleNVPair) { return value == ((DoubleNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new DoubleNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new DoubleNVPair(getName(), (Double) newValue);
    }
  }

  public static class FloatNVPair extends AbstractNVPair {
    private final float value;

    public FloatNVPair(String name, float value) {
      super(name);
      this.value = value;
    }

    public float getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.FLOAT;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof FloatNVPair) { return value == ((FloatNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new FloatNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new FloatNVPair(getName(), (Float) newValue);
    }
  }

  public static class IntNVPair extends AbstractNVPair {
    private final int value;

    public IntNVPair(String name, int value) {
      super(name);
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.INT;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof IntNVPair) { return value == ((IntNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new IntNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new IntNVPair(getName(), (Integer) newValue);
    }
  }

  public static class ShortNVPair extends AbstractNVPair {
    private final short value;

    public ShortNVPair(String name, short value) {
      super(name);
      this.value = value;
    }

    public short getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.SHORT;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof ShortNVPair) { return value == ((ShortNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new ShortNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new ShortNVPair(getName(), (Short) newValue);
    }
  }

  public static class LongNVPair extends AbstractNVPair {
    private final long value;

    public LongNVPair(String name, long value) {
      super(name);
      this.value = value;
    }

    public long getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof LongNVPair) { return value == ((LongNVPair) obj).value; }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new LongNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new LongNVPair(getName(), (Long) newValue);
    }
  }

  public static class StringNVPair extends AbstractNVPair {
    private final String value;

    public StringNVPair(String name, String value) {
      super(name);
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return value;
    }

    @Override
    public ValueType getType() {
      return ValueType.STRING;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof StringNVPair) { return value.equals(((StringNVPair) obj).value); }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new StringNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new StringNVPair(getName(), (String) newValue);
    }
  }

  public static class ByteArrayNVPair extends AbstractNVPair {
    private final byte[] value;

    public ByteArrayNVPair(String name, byte[] value) {
      super(name);
      this.value = value;
    }

    public byte[] getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      List<Byte> list = new ArrayList<Byte>(value.length);
      for (byte b : value) {
        list.add(b);
      }
      return list.toString();
    }

    @Override
    public ValueType getType() {
      return ValueType.BYTE_ARRAY;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof ByteArrayNVPair) { return Arrays.equals(value, ((ByteArrayNVPair) obj).value); }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new ByteArrayNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new ByteArrayNVPair(getName(), (byte[]) newValue);
    }
  }

  public static class DateNVPair extends AbstractNVPair {
    private final Date value;

    public DateNVPair(String name, Date value) {
      super(name);
      this.value = value;
    }

    public Date getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return value.toString();
    }

    @Override
    public ValueType getType() {
      return ValueType.DATE;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof DateNVPair) { return value.equals(((DateNVPair) obj).value); }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new DateNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new DateNVPair(getName(), (Date) newValue);
    }
  }

  public static class SqlDateNVPair extends AbstractNVPair {
    private final java.sql.Date value;

    public SqlDateNVPair(String name, java.sql.Date value) {
      super(name);
      this.value = value;
    }

    public java.sql.Date getValue() {
      return value;
    }

    public Object getObjectValue() {
      return value;
    }

    @Override
    public String valueAsString() {
      return value.toString();
    }

    @Override
    public ValueType getType() {
      return ValueType.SQL_DATE;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof SqlDateNVPair) { return value.equals(((SqlDateNVPair) obj).value); }
      return false;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new SqlDateNVPair(newName, value);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new SqlDateNVPair(getName(), (java.sql.Date) newValue);
    }
  }

  public static class EnumNVPair extends AbstractNVPair {

    private final String className;
    private final int    ordinal;

    public EnumNVPair(String name, Enum e) {
      this(name, e.getClass().getName(), e.ordinal());
    }

    public Object getObjectValue() {
      try {
        return Class.forName(className, false, Thread.currentThread().getContextClassLoader()).getEnumConstants()[ordinal];
      } catch (ClassNotFoundException e) {
        // XXX: Should CNFE be part of getObjectValue() signature? This runtime smells bad
        throw new RuntimeException(e);
      }
    }

    public EnumNVPair(String name, String className, int ordinal) {
      super(name);
      this.className = className;
      this.ordinal = ordinal;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof EnumNVPair) {
        EnumNVPair other = (EnumNVPair) obj;
        return ordinal == other.ordinal && className.equals(other.className);
      }

      return false;
    }

    @Override
    public String valueAsString() {
      return className + "(" + ordinal + ")";
    }

    @Override
    public ValueType getType() {
      return ValueType.ENUM;
    }

    public int getOrdinal() {
      return ordinal;
    }

    public String getClassName() {
      return className;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new EnumNVPair(newName, className, ordinal);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new EnumNVPair(getName(), (Enum) newValue);
    }

  }

  public static class NullNVPair extends AbstractNVPair {

    public NullNVPair(String name) {
      super(name);
    }

    public Object getObjectValue() {
      return null;
    }

    @Override
    boolean basicEquals(NVPair other) {
      return other instanceof NullNVPair;
    }

    @Override
    public String valueAsString() {
      // XXX: is this a good return value?
      return "null";
    }

    @Override
    public ValueType getType() {
      return ValueType.NULL;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new NullNVPair(newName);
    }

    public NVPair cloneWithNewValue(Object newValue) {
      if (newValue != null) { throw new IllegalArgumentException(); }
      return this;
    }
  }

  public static class ObjectIdNVPair extends AbstractNVPair {

    private final ObjectID oid;

    public ObjectIdNVPair(String name, ObjectID oid) {
      super(name);
      this.oid = oid;
    }

    public Object getObjectValue() {
      return oid;
    }

    public ObjectID getValue() {
      return oid;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new ObjectIdNVPair(newName, oid);
    }

    @Override
    boolean basicEquals(NVPair other) {
      if (other instanceof ObjectIdNVPair) { return oid.equals(((ObjectIdNVPair) other).oid); }
      return false;
    }

    @Override
    public String valueAsString() {
      return oid.toString();
    }

    @Override
    public ValueType getType() {
      return ValueType.OBJECT_ID;
    }

    public NVPair cloneWithNewValue(Object newValue) {
      return new ObjectIdNVPair(getName(), (ObjectID) newValue);
    }
  }

  public static NVPair createNVPair(String name, Object value, ValueType type) {
    if (value == null) return new NullNVPair(name);

    if (ValueType.ENUM.equals(type)) { return enumPairFromString(name, (String) value); }
    if (ValueType.CHAR.equals(type)) { return new AbstractNVPair.CharNVPair(name, (char) ((Integer) value).intValue()); }

    return AbstractNVPair.createNVPair(name, value);
  }

  public static EnumNVPair enumPairFromString(String name, String enumString) {
    String className = enumString.substring(0, enumString.length() - 10);
    int ordinal = Integer.parseInt(enumString.substring(enumString.length() - 10));
    return new EnumNVPair(name, className, ordinal);
  }

  public static String enumStorageString(EnumNVPair enumPair) {
    return enumStorageString(enumPair.getClassName(), enumPair.getOrdinal());
  }

  private static String enumStorageString(String className, int ordinal) {
    StringBuilder sb = new StringBuilder(className.length() + 10);
    sb.append(className);

    String ordinalString = String.valueOf(ordinal);
    for (int i = ordinalString.length(); i < 10; i++) {
      sb.append('0');
    }

    sb.append(ordinalString);
    return sb.toString();
  }

  public static NVPair createNVPair(String attributeName, Object value) {
    if (value == null) { return new NullNVPair(attributeName); }

    if (value instanceof Byte) {
      return new ByteNVPair(attributeName, (Byte) value);
    } else if (value instanceof Boolean) {
      return new BooleanNVPair(attributeName, (Boolean) value);
    } else if (value instanceof Character) {
      return new CharNVPair(attributeName, (Character) value);
    } else if (value instanceof Double) {
      return new DoubleNVPair(attributeName, (Double) value);
    } else if (value instanceof Float) {
      return new FloatNVPair(attributeName, (Float) value);
    } else if (value instanceof Integer) {
      return new IntNVPair(attributeName, (Integer) value);
    } else if (value instanceof Short) {
      return new ShortNVPair(attributeName, (Short) value);
    } else if (value instanceof Long) {
      return new LongNVPair(attributeName, (Long) value);
    } else if (value instanceof String) {
      return new StringNVPair(attributeName, (String) value);
    } else if (value instanceof byte[]) {
      return new ByteArrayNVPair(attributeName, (byte[]) value);
    } else if (value instanceof java.sql.Date) {
      // this one must come before regular java.util.Date
      return new SqlDateNVPair(attributeName, (java.sql.Date) value);
    } else if (value instanceof Date) {
      return new DateNVPair(attributeName, (Date) value);
    } else if (value instanceof ObjectID) {
      return new ObjectIdNVPair(attributeName, (ObjectID) value);
    } else if (value instanceof Enum) { return new EnumNVPair(attributeName, (Enum) value); }

    throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
  }
}
