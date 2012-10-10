package com.tc.objectserver.persistence.gb;

import org.terracotta.corestorage.Serializer;

import com.tc.util.AbstractIdentifier;

import java.nio.ByteBuffer;

/**
 * @author tim
 */
public abstract class AbstractIdentifierSerializer<K extends AbstractIdentifier> implements Serializer<K> {
  private final Class<K> c;

  protected AbstractIdentifierSerializer(final Class<K> c) {
    this.c = c;
  }

  protected abstract K createIdentifier(long id);

  @Override
  public K deserialize(final ByteBuffer buffer) {
    return createIdentifier(buffer.getLong());
  }

  @Override
  public ByteBuffer serialize(final K k) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
    buffer.putLong(k.toLong()).flip();
    return buffer;
  }

  @Override
  public boolean equals(final ByteBuffer left, final Object right) {
    if (c.isInstance(right)) {
      return left.getLong() == ((AbstractIdentifier) right).toLong();
    } else {
      return false;
    }
  }
}
