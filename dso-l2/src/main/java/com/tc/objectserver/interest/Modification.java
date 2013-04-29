/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.ObjectID;

class Modification {
  private final Object key;
  private final ObjectID objectId;
  private final String cacheName;
  private ModificationType type;
  private byte[] value;

  Modification(final ModificationType type, final Object key,
               final ObjectID objectId, final String cacheName) {
    this(type, key, objectId, null, cacheName);
  }

  Modification(final ModificationType type, final Object key, final ObjectID objectId,
               final byte[] value, final String cacheName) {
    this.type = type;
    this.key = key;
    this.objectId = objectId;
    this.value = value;
    this.cacheName = cacheName;
  }

  ModificationType getType() {
    return type;
  }

  void setType(final ModificationType type) {
    this.type = type;
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

  String getCacheName() {
    return cacheName;
  }
}
