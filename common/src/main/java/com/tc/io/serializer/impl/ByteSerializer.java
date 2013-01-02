/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Byte
 */
public final class ByteSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeByte(((Byte)o).byteValue());
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Byte.valueOf(in.readByte());
  }

  @Override
  public byte getSerializerID() {
    return BYTE;
  }

}
