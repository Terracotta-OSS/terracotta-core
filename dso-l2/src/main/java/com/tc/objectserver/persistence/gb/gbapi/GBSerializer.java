package com.tc.objectserver.persistence.gb.gbapi;

import java.nio.ByteBuffer;

/**
 * @author tim
 */
public interface GBSerializer<T> {
  public T deserialize(ByteBuffer buffer);

  public ByteBuffer serialize(T t);

  public boolean equals(ByteBuffer left, Object right);
}
