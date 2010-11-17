/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCSerializable;

public interface NVPair extends TCSerializable {

  String getName();

  ValueType getType();

  Object getObjectValue();

  NVPair cloneWithNewName(String newName);

  // XXX: remove this from the interface?
  String valueAsString();
}