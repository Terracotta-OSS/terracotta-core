package org.terracotta.helper.common;

import org.terracotta.entity.EntityResponse;

import java.nio.ByteBuffer;

public class HelperEntityResponse implements EntityResponse {
  private final HelperEntityMessageType type;

  public HelperEntityResponse(final HelperEntityMessageType type) {this.type = type;}

  public static HelperEntityResponse decode(byte[] bytes) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    int ordinal = byteBuffer.getInt();
    return new HelperEntityResponse(HelperEntityMessageType.values()[ordinal]);
  }

  public byte[] encode() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
    byteBuffer.putInt(type.ordinal());

    return byteBuffer.array();
  }

  public HelperEntityMessageType getType() {
    return type;
  }
}
