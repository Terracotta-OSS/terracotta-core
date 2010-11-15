/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.metadata.AbstractNVPair.EnumNVPair;
import com.tc.util.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class hold the Metadata information associated with a shared object.
 * 
 * @author Nabib
 */
public class MetaDataDescriptorImpl implements TCSerializable, MetaDataDescriptorInternal {

  private static final MetaDataDescriptorImpl TEMPLATE    = new MetaDataDescriptorImpl("template");
  public static final MetaDataDescriptor[]    EMPTY_ARRAY = new MetaDataDescriptor[] {};

  private final String                        category;
  private final List<AbstractNVPair>                  metaDatas;

  public MetaDataDescriptorImpl(String category) {
    this(category, new ArrayList<AbstractNVPair>());
  }

  private MetaDataDescriptorImpl(String category, List<AbstractNVPair> metaDatas) {
    this.category = category;
    this.metaDatas = metaDatas;
  }

  public List<AbstractNVPair> getMetaDatas() {
    return metaDatas;
  }

  public String getCategory() {
    return this.category;
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    final String cat = in.readString();
    final int size = in.readInt();
    List<AbstractNVPair> data = new ArrayList<AbstractNVPair>(size);

    for (int i = 0; i < size; i++) {
      data.add(AbstractNVPair.deserializeInstance(in));
    }

    return new MetaDataDescriptorImpl(cat, data);
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeString(category);
    out.writeInt(metaDatas.size());
    for (AbstractNVPair nvpair : metaDatas) {
      nvpair.serializeTo(out);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + category + "): " + metaDatas.toString();
  }

  public static MetaDataDescriptorInternal deserializeInstance(TCByteBufferInputStream in) throws IOException {
    return (MetaDataDescriptorInternal) TEMPLATE.deserializeFrom(in);
  }

  public void add(String name, boolean value) {
    metaDatas.add(new AbstractNVPair.BooleanNVPair(name, value));
  }

  public void add(String name, byte value) {
    metaDatas.add(new AbstractNVPair.ByteNVPair(name, value));
  }

  public void add(String name, char value) {
    metaDatas.add(new AbstractNVPair.CharNVPair(name, value));
  }

  public void add(String name, double value) {
    metaDatas.add(new AbstractNVPair.DoubleNVPair(name, value));
  }

  public void add(String name, float value) {
    metaDatas.add(new AbstractNVPair.FloatNVPair(name, value));
  }

  public void add(String name, int value) {
    metaDatas.add(new AbstractNVPair.IntNVPair(name, value));
  }

  public void add(String name, long value) {
    metaDatas.add(new AbstractNVPair.LongNVPair(name, value));
  }

  public void add(String name, short value) {
    metaDatas.add(new AbstractNVPair.ShortNVPair(name, value));
  }

  public void add(String name, String value) {
    metaDatas.add(new AbstractNVPair.StringNVPair(name, value));
  }

  public void add(String name, byte[] value) {
    metaDatas.add(new AbstractNVPair.ByteArrayNVPair(name, value));
  }

  public void add(String name, Enum value) {
    metaDatas.add(new EnumNVPair(name, value));
  }

  public void add(String name, Date value) {
    metaDatas.add(new AbstractNVPair.DateNVPair(name, value));
  }

  public void add(String name, Object value) {
    Class type = value.getClass();
    ValueType vt = TYPES.get(type);

    if (vt != null) {
      switch (vt) {
        case BOOLEAN: {
          add(name, ((Boolean) value).booleanValue());
          break;
        }
        case BYTE: {
          add(name, ((Byte) value).byteValue());
          break;
        }
        case CHAR: {
          add(name, ((Character) value).charValue());
          break;
        }
        case DOUBLE: {
          add(name, ((Double) value).doubleValue());
          break;
        }
        case FLOAT: {
          add(name, ((Float) value).floatValue());
          break;
        }
        case INT: {
          add(name, ((Integer) value).intValue());
          break;
        }
        case LONG: {
          add(name, ((Long) value).longValue());
          break;
        }
        case SHORT: {
          add(name, ((Short) value).shortValue());
          break;
        }
        case DATE: {
          add(name, (Date) value);
          break;
        }
        case BYTE_ARRAY: {
          add(name, ((byte[]) value));
          break;
        }
        case STRING: {
          add(name, (String) value);
          break;
        }
        case ENUM: {
          throw new AssertionError();
        }
      }

      return;
    }

    // might be an enum
    if (ClassUtils.isDsoEnum(type)) {
      add(name, (Enum) value);
      return;
    }

    throw new IllegalArgumentException("Unsupported type: " + type);
  }

  public int size() {
    return metaDatas.size();
  }

  private static final Map<Class, ValueType> TYPES;

  static {
    Map<Class, ValueType> map = new HashMap<Class, ValueType>();
    map.put(Byte.class, ValueType.BYTE);
    map.put(Boolean.class, ValueType.BOOLEAN);
    map.put(Character.class, ValueType.CHAR);
    map.put(Double.class, ValueType.DOUBLE);
    map.put(Float.class, ValueType.FLOAT);
    map.put(Integer.class, ValueType.INT);
    map.put(Short.class, ValueType.SHORT);
    map.put(Long.class, ValueType.LONG);
    map.put(String.class, ValueType.STRING);
    map.put(Date.class, ValueType.DATE);
    map.put(byte[].class, ValueType.BYTE_ARRAY);

    TYPES = map;
  }

}
