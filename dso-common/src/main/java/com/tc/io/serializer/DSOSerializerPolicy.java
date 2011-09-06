/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io.serializer;

import com.tc.io.serializer.api.Serializer;
import com.tc.io.serializer.api.SerializerPolicy;
import com.tc.io.serializer.impl.BooleanSerializer;
import com.tc.io.serializer.impl.ByteSerializer;
import com.tc.io.serializer.impl.CharacterSerializer;
import com.tc.io.serializer.impl.DoubleSerializer;
import com.tc.io.serializer.impl.FloatSerializer;
import com.tc.io.serializer.impl.IntegerSerializer;
import com.tc.io.serializer.impl.LongSerializer;
import com.tc.io.serializer.impl.ObjectIDSerializer;
import com.tc.io.serializer.impl.ObjectSerializer;
import com.tc.io.serializer.impl.ShortSerializer;
import com.tc.io.serializer.impl.StringUTFSerializer;
import com.tc.object.ObjectID;

import gnu.trove.TIntObjectHashMap;

import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

public final class DSOSerializerPolicy implements SerializerPolicy {

  private final Map                       class2SerializerDescriptor;
  private final TIntObjectHashMap         id2Serializer;
  private final DSOSerializerPolicy.SDesc objectSerializer;

  public DSOSerializerPolicy() {
    class2SerializerDescriptor = new HashMap();
    id2Serializer = new TIntObjectHashMap();

    addSerializerMapping(Object.class, new ObjectSerializer());
    addSerializerMapping(ObjectID.class, new ObjectIDSerializer());
    addSerializerMapping(Boolean.class, new BooleanSerializer());
    addSerializerMapping(Byte.class, new ByteSerializer());
    addSerializerMapping(Character.class, new CharacterSerializer());
    addSerializerMapping(String.class, new StringUTFSerializer());
    addSerializerMapping(Double.class, new DoubleSerializer());
    addSerializerMapping(Float.class, new FloatSerializer());
    addSerializerMapping(Integer.class, new IntegerSerializer());
    addSerializerMapping(Long.class, new LongSerializer());
    addSerializerMapping(Short.class, new ShortSerializer());

    objectSerializer = (SDesc) class2SerializerDescriptor.get(Object.class);
  }

  private void addSerializerMapping(Class clazz, Serializer serializer) {
    SDesc desc = new SDesc(clazz, serializer);
    if (id2Serializer.containsKey(desc.getID())) throw new AssertionError("Duplicate desc ids: " + desc.getID());
    id2Serializer.put(desc.getID(), desc.serializer);
    class2SerializerDescriptor.put(desc.clazz, desc);
  }

  public Serializer getSerializerFor(Object o, ObjectOutput out) throws IOException {
    DSOSerializerPolicy.SDesc desc = null;
    if (o != null) {
      desc = (DSOSerializerPolicy.SDesc) class2SerializerDescriptor.get(o.getClass());
    }
    desc = (desc == null) ? objectSerializer : desc;
    return extractSerializer(desc, out);
  }

  private Serializer extractSerializer(DSOSerializerPolicy.SDesc desc, DataOutput out) throws IOException {
    out.writeByte(desc.getID());
    return desc.serializer;
  }

  public Serializer getSerializerFor(ObjectInput in) throws IOException {
    Serializer rv = (Serializer) id2Serializer.get(in.readByte());
    return rv == null ? objectSerializer.serializer : rv;
  }

  static final class SDesc {
    Class      clazz;
    Serializer serializer;

    SDesc(Class clazz, Serializer serializer) {
      this.clazz = clazz;
      this.serializer = serializer;
    }

    int getID() {
      return serializer.getSerializerID();
    }
  }
}