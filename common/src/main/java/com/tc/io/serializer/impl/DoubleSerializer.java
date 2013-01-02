/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Double
 */
public final class DoubleSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeDouble(((Double)o).doubleValue());
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Double.valueOf(in.readDouble());
  }

  @Override
  public byte getSerializerID() {
    return DOUBLE;
  }

}
