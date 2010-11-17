/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

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

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    String readName = in.readString();
    byte ordinal = in.readByte();
    ValueType type = ALL_TYPES[ordinal];
    return type.deserializeFrom(readName, in);
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeString(getName());

    ValueType type = getType();

    out.writeByte(type.ordinal());
    type.serializeTo(this, out);
  }

  public abstract ValueType getType();

  public static AbstractNVPair deserializeInstance(TCByteBufferInput in) throws IOException {
    return (AbstractNVPair) TEMPLATE.deserializeFrom(in);
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
      throw new UnsupportedOperationException();
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
  }

  public static class EnumNVPair extends AbstractNVPair {

    private final String enumClass;
    private final String enumName;

    public EnumNVPair(String name, Enum e) {
      this(name, e.getClass().getName(), e.name());
    }

    public Object getObjectValue() {
      return getEnumName();
    }

    public EnumNVPair(String name, String enumClass, String enumName) {
      super(name);
      this.enumClass = enumClass;
      this.enumName = enumName;
    }

    @Override
    boolean basicEquals(NVPair obj) {
      if (obj instanceof EnumNVPair) {
        EnumNVPair other = (EnumNVPair) obj;
        return enumClass.equals(other.enumClass) && enumName.equals(other.enumName);
      }

      return false;
    }

    @Override
    public String valueAsString() {
      return enumClass + "(" + enumName + ")";
    }

    @Override
    public ValueType getType() {
      return ValueType.ENUM;
    }

    public String getEnumClass() {
      return enumClass;
    }

    public String getEnumName() {
      return enumName;
    }

    @Override
    public NVPair cloneWithNewName(String newName) {
      return new EnumNVPair(newName, enumClass, enumName);
    }
  }

  public static NVPair createNVPair(String attributeName, Object value) {
    NVPair pair = null;
    if (value instanceof Byte) {
      pair = new ByteNVPair(attributeName, (Byte) value);
    } else if (value instanceof Boolean) {
      pair = new BooleanNVPair(attributeName, (Boolean) value);
    } else if (value instanceof Character) {
      pair = new CharNVPair(attributeName, (Character) value);
    } else if (value instanceof Double) {
      pair = new DoubleNVPair(attributeName, (Double) value);
    } else if (value instanceof Float) {
      pair = new FloatNVPair(attributeName, (Float) value);
    } else if (value instanceof Integer) {
      pair = new IntNVPair(attributeName, (Integer) value);
    } else if (value instanceof Short) {
      pair = new ShortNVPair(attributeName, (Short) value);
    } else if (value instanceof Long) {
      pair = new LongNVPair(attributeName, (Long) value);
    } else if (value instanceof String) {
      pair = new StringNVPair(attributeName, (String) value);
    } else if (value instanceof byte[]) {
      pair = new ByteArrayNVPair(attributeName, (byte[]) value);
    } else if (value instanceof Date) {
      pair = new DateNVPair(attributeName, (Date) value);
    } else if (value instanceof Enum) {
      pair = new EnumNVPair(attributeName, (Enum) value);
    }
    return pair;
  }

}
