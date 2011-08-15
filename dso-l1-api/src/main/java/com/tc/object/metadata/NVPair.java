/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.impl.ObjectStringSerializer;

import java.io.Serializable;

public interface NVPair extends Serializable {

  String getName();

  ValueType getType();

  Object getObjectValue();

  NVPair cloneWithNewName(String newName);

  NVPair cloneWithNewValue(Object newValue);

  // XXX: remove this from the interface?
  String valueAsString();

  void serializeTo(TCByteBufferOutput out, ObjectStringSerializer serializer);
}