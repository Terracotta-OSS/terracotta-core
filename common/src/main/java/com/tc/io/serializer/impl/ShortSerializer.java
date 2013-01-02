/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Short
 */
public final class ShortSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeShort(((Short)o).shortValue());
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Short.valueOf(in.readShort());
  }

  @Override
  public byte getSerializerID() {
    return SHORT;
  }

}
