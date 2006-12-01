/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;
import com.tc.io.serializer.api.StringIndex;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Strings
 */
public final class StringSerializer implements Serializer {

  private final StringIndex stringIndex;

  public StringSerializer(StringIndex stringIndex) {
    this.stringIndex = stringIndex;
  }

  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeLong(this.stringIndex.getOrCreateIndexFor((String) o));
  }

  public Object deserializeFrom(ObjectInput in) throws IOException {
    return this.stringIndex.getStringFor(in.readLong());
  }

  public byte getSerializerID() {
    return STRING_INDEX;
  }
}
