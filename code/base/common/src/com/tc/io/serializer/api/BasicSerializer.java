/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BasicSerializer implements Serializer {

  private final SerializerPolicy serializers;

  public BasicSerializer(SerializerPolicy serializers) {
    this.serializers = serializers;
  }

  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    serializers.getSerializerFor(o, out).serializeTo(o, out);
  }

  public Object deserializeFrom(ObjectInput in) throws IOException, ClassNotFoundException {
    return serializers.getSerializerFor(in).deserializeFrom(in);
  }

  public byte getSerializerID() {
    return UNKNOWN;
  }

}
