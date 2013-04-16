/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.ObjectID;

/**
 * @author Eugene Shelestovich
 */
public class Modification {
  private final ModificationType type;
  private final Object key;
  private final ObjectID objectId;
  private byte[] value;

  Modification(final ModificationType type, final Object key, final ObjectID objectId) {
    this(type, key, objectId, null);
  }

  Modification(final ModificationType type, final Object key, final ObjectID objectId, final byte[] value) {
    this.type = type;
    this.key = key;
    this.objectId = objectId;
    this.value = value;
  }

  ModificationType getType() {
    return type;
  }

  Object getKey() {
    return key;
  }

  ObjectID getObjectId() {
    return objectId;
  }

  byte[] getValue() {
    return value;
  }

  void setValue(final byte[] value) {
    this.value = value;
  }
}
