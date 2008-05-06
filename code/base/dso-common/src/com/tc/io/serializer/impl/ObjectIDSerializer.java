/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.impl;

import com.tc.io.serializer.api.Serializer;
import com.tc.object.ObjectID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * ObjectIDs
 */
public final class ObjectIDSerializer implements Serializer {

  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    out.writeLong(((ObjectID) o).toLong());
  }

  public Object deserializeFrom(ObjectInput in) throws IOException {
    return new ObjectID(in.readLong());
  }

  public byte getSerializerID() {
    return OBJECT_ID;
  }
}