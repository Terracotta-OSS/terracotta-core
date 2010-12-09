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

  // XXX: remove this from the interface?
  String valueAsString();
}