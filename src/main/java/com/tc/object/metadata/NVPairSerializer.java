/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.terracottatech.search.AbstractNVPair.BooleanNVPair;
import com.terracottatech.search.AbstractNVPair.ByteArrayNVPair;
import com.terracottatech.search.AbstractNVPair.ByteNVPair;
import com.terracottatech.search.AbstractNVPair.CharNVPair;
import com.terracottatech.search.AbstractNVPair.DateNVPair;
import com.terracottatech.search.AbstractNVPair.DoubleNVPair;
import com.terracottatech.search.AbstractNVPair.EnumNVPair;
import com.terracottatech.search.AbstractNVPair.FloatNVPair;
import com.terracottatech.search.AbstractNVPair.IntNVPair;
import com.terracottatech.search.AbstractNVPair.LongNVPair;
import com.terracottatech.search.AbstractNVPair.NullNVPair;
import com.terracottatech.search.AbstractNVPair.ShortNVPair;
import com.terracottatech.search.AbstractNVPair.SqlDateNVPair;
import com.terracottatech.search.AbstractNVPair.StringNVPair;
import com.terracottatech.search.AbstractNVPair.ValueIdNVPair;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;
import com.terracottatech.search.ValueType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class NVPairSerializer {

  private static final ValueType[] ALL_TYPES = ValueType.values();

  public void serialize(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer stringSerializer) {
    stringSerializer.writeString(out, nvPair.getName());

    ValueType type = nvPair.getType();
    out.writeByte(type.ordinal());

    switch (type) {
      case BOOLEAN:
        out.writeBoolean(((BooleanNVPair) nvPair).getValue());
        return;
      case BYTE:
        out.writeByte(((ByteNVPair) nvPair).getValue());
        return;
      case BYTE_ARRAY:
        byte[] val = ((ByteArrayNVPair) nvPair).getValue();
        out.writeInt(val.length);
        out.write(val);
        return;
      case CHAR:
        out.writeChar(((CharNVPair) nvPair).getValue());
        return;
      case DATE:
        out.writeLong(((DateNVPair) nvPair).getValue().getTime());
        return;
      case DOUBLE:
        out.writeDouble(((DoubleNVPair) nvPair).getValue());
        return;
      case ENUM:
        EnumNVPair enumPair = (EnumNVPair) nvPair;
        stringSerializer.writeString(out, enumPair.getClassName());
        out.writeInt(enumPair.getOrdinal());
        return;
      case FLOAT:
        out.writeFloat(((FloatNVPair) nvPair).getValue());
        return;
      case INT:
        out.writeInt(((IntNVPair) nvPair).getValue());
        return;
      case LONG:
        out.writeLong(((LongNVPair) nvPair).getValue());
        return;
      case NULL:
        // no state
        return;
      case SHORT:
        out.writeShort(((ShortNVPair) nvPair).getValue());
        return;
      case SQL_DATE:
        out.writeLong(((SqlDateNVPair) nvPair).getValue().getTime());
        return;
      case STRING:
        String value = ((StringNVPair) nvPair).getValue();
        try {
          stringSerializer.writeStringBytes(out, value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new AssertionError(e);
        }
        return;
      case VALUE_ID:
        out.writeLong(((ValueIdNVPair) nvPair).getValue().toLong());
        return;
    }

    throw new AssertionError("Unknown type: " + type);
  }

  public NVPair deserialize(TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
    String name = serializer.readString(in);
    byte ordinal = in.readByte();

    ValueType type = ALL_TYPES[ordinal];
    switch (type) {
      case BOOLEAN:
        return new BooleanNVPair(name, in.readBoolean());
      case BYTE:
        return new ByteNVPair(name, in.readByte());
      case BYTE_ARRAY:
        int len = in.readInt();
        byte[] data = new byte[len];
        in.read(data, 0, len);
        return new ByteArrayNVPair(name, data);
      case CHAR:
        return new CharNVPair(name, in.readChar());
      case DATE:
        return new DateNVPair(name, new java.util.Date(in.readLong()));
      case DOUBLE:
        return new DoubleNVPair(name, in.readDouble());
      case ENUM:
        String className = serializer.readString(in);
        int enumOrdinal = in.readInt();
        return new EnumNVPair(name, className, enumOrdinal);
      case FLOAT:
        return new FloatNVPair(name, in.readFloat());
      case INT:
        return new IntNVPair(name, in.readInt());
      case LONG:
        return new LongNVPair(name, in.readLong());
      case NULL:
        return new NullNVPair(name);
      case SHORT:
        return new ShortNVPair(name, in.readShort());
      case SQL_DATE:
        return new SqlDateNVPair(name, new java.sql.Date(in.readLong()));
      case STRING:
        return new StringNVPair(name, new String(serializer.readStringBytes(in), "UTF-8"));
      case VALUE_ID:
        return new ValueIdNVPair(name, new ValueID(in.readLong()));
    }

    throw new AssertionError("Unknown type: " + type);
  }

  static {
    int length = ValueType.values().length;
    if (length > 127) {
      // The encoding logic could support all 256 values in the encoded byte or we could expand to 2 bytes if needed
      throw new AssertionError("Current implementation does not allow for more 127 types");
    }
  }

}
