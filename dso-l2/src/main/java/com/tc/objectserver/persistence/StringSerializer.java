package com.tc.objectserver.persistence;

import org.terracotta.corestorage.Serializer;

import java.nio.ByteBuffer;

/**
* @author tim
*/
class StringSerializer implements Serializer<String> {
  public static final StringSerializer INSTANCE = new StringSerializer();

  @Override
  public String deserialize(final ByteBuffer buffer) {
    return buffer.asCharBuffer().toString();
  }

  @Override
  public ByteBuffer serialize(final String s) {
    ByteBuffer buffer = ByteBuffer.allocate(s.length() * 2);
    buffer.asCharBuffer().put(s).clear();
    return buffer;
  }

  @Override
  public boolean equals(final ByteBuffer left, final Object right) {
    if (right instanceof String) {
      return deserialize(left).equals(right);
    } else {
      return false;
    }
  }
}
