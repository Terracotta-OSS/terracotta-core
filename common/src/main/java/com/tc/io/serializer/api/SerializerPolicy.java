/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.io.serializer.api;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public interface SerializerPolicy {
  
  public Serializer getSerializerFor(Object o, ObjectOutput out) throws IOException;
  public Serializer getSerializerFor(ObjectInput in) throws IOException;
}
