package org.terracotta.helper.common;

import org.terracotta.entity.EntityMessage;

import java.nio.ByteBuffer;

public class HelperEntityMessage implements EntityMessage {
  private final HelperEntityMessageType type;

  public HelperEntityMessage(final HelperEntityMessageType type) {this.type = type;}
  
  public static HelperEntityMessage decode(byte[] bytes) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    int ordinal = byteBuffer.getInt();
    return new HelperEntityMessage(HelperEntityMessageType.values()[ordinal]);
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
