/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Boolean
 */
public final class BooleanSerializer implements Serializer {

  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    boolean b = ((Boolean)o).booleanValue();
    out.writeBoolean(b);
  }

  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Boolean.valueOf(in.readBoolean());
  }

  public byte getSerializerID() {
    return BOOLEAN;
  }

}
