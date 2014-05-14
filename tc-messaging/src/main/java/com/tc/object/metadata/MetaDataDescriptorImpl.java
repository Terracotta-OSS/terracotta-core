/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.util.ClassUtils;
import com.terracottatech.search.AbstractNVPair;
import com.terracottatech.search.AbstractNVPair.EnumNVPair;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;
import com.terracottatech.search.ValueType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * This class holds a collection od metadata
 * 
 * @author teck
 */
public class MetaDataDescriptorImpl implements MetaDataDescriptorInternal {

  private static final NVPairSerializer       NVPAIR_SERIALIZER = new NVPairSerializer();
  private static final MetaDataDescriptorImpl TEMPLATE          = new MetaDataDescriptorImpl("template");
  public static final MetaDataDescriptor[]    EMPTY_ARRAY       = new MetaDataDescriptor[] {};

  private final String                        category;
  private final List<NVPair>                  metaDatas;
  private ObjectID                            oid;

  public MetaDataDescriptorImpl(String category) {
    this(category, new ArrayList<NVPair>(), ObjectID.NULL_ID);
  }

  private MetaDataDescriptorImpl(String category, List<NVPair> metaDatas, ObjectID oid) {
    this.category = category;
    this.metaDatas = metaDatas;
    this.oid = oid;
  }

  @Override
  public Iterator<NVPair> getMetaDatas() {
    return metaDatas.iterator();
  }

  @Override
  public int numberOfNvPairs() {
    return metaDatas.size();
  }

  @Override
  public String getCategory() {
    return this.category;
  }

  @Override
  public ObjectID getObjectId() {
    return oid;
  }

  @Override
  public void setObjectID(ObjectID id) {
    this.oid = id;
  }

  public Object deserializeFrom(TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
    final String cat = serializer.readString(in);
    final ObjectID id = new ObjectID(in.readLong());

    final int size = in.readInt();
    List<NVPair> data = new ArrayList<NVPair>(size);
    for (int i = 0; i < size; i++) {
      data.add(NVPAIR_SERIALIZER.deserialize(in, serializer));
    }

    return new MetaDataDescriptorImpl(cat, Collections.unmodifiableList(data), id);
  }

  @Override
  public void serializeTo(TCByteBufferOutput out, ObjectStringSerializer serializer) {
    serializer.writeString(out, category);

    if (oid.isNull()) { throw new AssertionError("OID never set"); }
    out.writeLong(oid.toLong());

    out.writeInt(metaDatas.size());
    for (NVPair nvpair : metaDatas) {
      NVPAIR_SERIALIZER.serialize(nvpair, out, serializer);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + category + "): " + metaDatas.toString();
  }

  public static MetaDataDescriptorInternal deserializeInstance(TCByteBufferInputStream in,
                                                               ObjectStringSerializer serializer) throws IOException {
    return (MetaDataDescriptorInternal) TEMPLATE.deserializeFrom(in, serializer);
  }

  @Override
  public void add(String name, boolean value) {
    metaDatas.add(new AbstractNVPair.BooleanNVPair(name, value));
  }

  @Override
  public void add(String name, byte value) {
    metaDatas.add(new AbstractNVPair.ByteNVPair(name, value));
  }

  @Override
  public void add(String name, char value) {
    metaDatas.add(new AbstractNVPair.CharNVPair(name, value));
  }

  @Override
  public void add(String name, double value) {
    metaDatas.add(new AbstractNVPair.DoubleNVPair(name, value));
  }

  @Override
  public void add(String name, float value) {
    metaDatas.add(new AbstractNVPair.FloatNVPair(name, value));
  }

  @Override
  public void add(String name, int value) {
    metaDatas.add(new AbstractNVPair.IntNVPair(name, value));
  }

  @Override
  public void add(String name, long value) {
    metaDatas.add(new AbstractNVPair.LongNVPair(name, value));
  }

  @Override
  public void add(String name, short value) {
    metaDatas.add(new AbstractNVPair.ShortNVPair(name, value));
  }

  @Override
  public void add(String name, String value) {
    metaDatas.add(new AbstractNVPair.StringNVPair(name, value));
  }

  @Override
  public void add(String name, byte[] value) {
    metaDatas.add(new AbstractNVPair.ByteArrayNVPair(name, value));
  }

  @Override
  public void add(String name, Enum value) {
    metaDatas.add(new EnumNVPair(name, value));
  }

  @Override
  public void add(String name, Date value) {
    metaDatas.add(new AbstractNVPair.DateNVPair(name, value));
  }

  @Override
  public void add(String name, java.sql.Date value) {
    metaDatas.add(new AbstractNVPair.SqlDateNVPair(name, value));
  }

  @Override
  public void add(String name, ObjectID value) {
    metaDatas.add(new AbstractNVPair.ValueIdNVPair(name, new ValueID(value.toLong())));
  }

  @Override
  public void addNull(String name) {
    metaDatas.add(new AbstractNVPair.NullNVPair(name));
  }

  @Override
  public void set(String name, Object value) {
    for (ListIterator<NVPair> iter = metaDatas.listIterator(); iter.hasNext();) {
      NVPair nvPair = iter.next();

      if (nvPair.getName().equals(name)) {
        iter.set(nvPair.cloneWithNewValue(value));
      }
    }
  }

  @Override
  public void add(String name, Object value) {
    if (value == null) {
      addNull(name);
      return;
    }

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
        case SQL_DATE: {
          add(name, (java.sql.Date) value);
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
        case NULL: {
          throw new AssertionError();
        }
        case VALUE_ID: {
          add(name, (ObjectID) value);
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
    map.put(java.sql.Date.class, ValueType.SQL_DATE);
    map.put(byte[].class, ValueType.BYTE_ARRAY);
    map.put(ObjectID.class, ValueType.VALUE_ID);

    TYPES = map;
  }

}
