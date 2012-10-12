package com.tc.objectserver.persistence;

import org.terracotta.corestorage.Serializer;

import java.nio.ByteBuffer;

/**
* @author tim
*/
public class LongSerializer implements Serializer<Long> {
  public static LongSerializer INSTANCE = new LongSerializer();

  @Override
  public Long deserialize(final ByteBuffer buffer) {
    return buffer.getLong();
  }

  @Override
  public ByteBuffer serialize(final Long aLong) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
    buffer.putLong(aLong).flip();
    return buffer;
  }

  @Override
  public boolean equals(final ByteBuffer left, final Object right) {
    if (right instanceof Long) {
      return deserialize(left).equals(right);
    } else {
      return false;
    }
  }
}
