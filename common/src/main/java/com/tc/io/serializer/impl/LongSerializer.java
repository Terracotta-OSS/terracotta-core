/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Long
 */
public final class LongSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeLong(((Long)o).longValue());
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Long.valueOf(in.readLong());
  }

  @Override
  public byte getSerializerID() {
    return LONG;
  }

}
