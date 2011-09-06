/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.AbstractNVPair.BooleanNVPair;
import com.tc.object.metadata.AbstractNVPair.ByteArrayNVPair;
import com.tc.object.metadata.AbstractNVPair.ByteNVPair;
import com.tc.object.metadata.AbstractNVPair.CharNVPair;
import com.tc.object.metadata.AbstractNVPair.DateNVPair;
import com.tc.object.metadata.AbstractNVPair.DoubleNVPair;
import com.tc.object.metadata.AbstractNVPair.EnumNVPair;
import com.tc.object.metadata.AbstractNVPair.FloatNVPair;
import com.tc.object.metadata.AbstractNVPair.IntNVPair;
import com.tc.object.metadata.AbstractNVPair.LongNVPair;
import com.tc.object.metadata.AbstractNVPair.NullNVPair;
import com.tc.object.metadata.AbstractNVPair.ObjectIdNVPair;
import com.tc.object.metadata.AbstractNVPair.ShortNVPair;
import com.tc.object.metadata.AbstractNVPair.SqlDateNVPair;
import com.tc.object.metadata.AbstractNVPair.StringNVPair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

public enum ValueType {
  OBJECT_ID {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new ObjectIdNVPair(name, new ObjectID(in.readLong()));
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeLong(((ObjectIdNVPair) nvPair).getValue().toLong());
    }
  },

  NULL {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) {
      return new NullNVPair(name);
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      // no state
    }
  },

  BOOLEAN {
    @Override
    public NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer)
        throws IOException {
      return new BooleanNVPair(name, in.readBoolean());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeBoolean(((BooleanNVPair) nvPair).getValue());
    }
  },

  BYTE {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new ByteNVPair(name, in.readByte());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeByte(((ByteNVPair) nvPair).getValue());
    }
  },

  CHAR {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new CharNVPair(name, in.readChar());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeChar(((CharNVPair) nvPair).getValue());
    }
  },

  DOUBLE {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new DoubleNVPair(name, in.readDouble());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeDouble(((DoubleNVPair) nvPair).getValue());
    }
  },

  FLOAT {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new FloatNVPair(name, in.readFloat());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeFloat(((FloatNVPair) nvPair).getValue());
    }
  },

  INT {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new IntNVPair(name, in.readInt());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeInt(((IntNVPair) nvPair).getValue());
    }
  },

  SHORT {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new ShortNVPair(name, in.readShort());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeShort(((ShortNVPair) nvPair).getValue());
    }
  },

  LONG {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new LongNVPair(name, in.readLong());
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeLong(((LongNVPair) nvPair).getValue());
    }
  },

  STRING {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new StringNVPair(name, new String(serializer.readStringBytes(in), "UTF-8"));
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      String value = ((StringNVPair) nvPair).getValue();
      try {
        serializer.writeStringBytes(out, value.getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    }
  },

  DATE {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new DateNVPair(name, new Date(in.readLong()));
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeLong(((DateNVPair) nvPair).getValue().getTime());
    }
  },

  SQL_DATE {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      return new SqlDateNVPair(name, new java.sql.Date(in.readLong()));
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      out.writeLong(((SqlDateNVPair) nvPair).getValue().getTime());
    }
  },

  BYTE_ARRAY {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      int len = in.readInt();
      byte[] data = new byte[len];
      in.read(data, 0, len);
      return new ByteArrayNVPair(name, data);
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      byte[] val = ((ByteArrayNVPair) nvPair).getValue();
      out.writeInt(val.length);
      out.write(val);
    }
  },

  ENUM {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer) throws IOException {
      String className = serializer.readString(in);
      int ordinal = in.readInt();
      return new EnumNVPair(name, className, ordinal);
    }

    @Override
    void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer) {
      EnumNVPair enumPair = (EnumNVPair) nvPair;
      serializer.writeString(out, enumPair.getClassName());
      out.writeInt(enumPair.getOrdinal());
    }

  };

  static {
    int length = ValueType.values().length;
    if (length > 127) {
      // The encoding logic could support all 256 values in the encoded byte or we could expand to 2 bytes if needed
      throw new AssertionError("Current implementation does not allow for more 127 types");
    }
  }

  abstract NVPair deserializeFrom(String name, TCByteBufferInput in, ObjectStringSerializer serializer)
      throws IOException;

  abstract void serializeTo(NVPair nvPair, TCByteBufferOutput out, ObjectStringSerializer serializer);

}
