/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCSerializable;

import java.io.Serializable;

public interface NVPair extends TCSerializable, Serializable {

  String getName();

  ValueType getType();

  Object getObjectValue();

  NVPair cloneWithNewName(String newName);

  NVPair cloneWithNewValue(Object newValue);

  // XXX: remove this from the interface?
  String valueAsString();
}