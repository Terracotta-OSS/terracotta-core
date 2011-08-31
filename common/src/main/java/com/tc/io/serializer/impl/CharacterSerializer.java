/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Character
 */
public final class CharacterSerializer implements Serializer {

  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeChar(((Character)o).charValue());
  }

  public Object deserializeFrom(ObjectInput in) throws IOException {
    return Character.valueOf(in.readChar());
  }

  public byte getSerializerID() {
    return CHARACTER;
  }

}