/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.google.common.base.Objects;

public class VersionedObject {

  private final Object object;
  private final long   version;

  public VersionedObject(final Object object, final long version) {
    this.object = object;
    this.version = version;
  }

  public Object getObject() {
    return this.object;
  }

  public long getVersion() {
    return this.version;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("object", object)
        .add("version", version)
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final VersionedObject that = (VersionedObject)o;

    return version == that.version && object.equals(that.object);

  }

  @Override
  public int hashCode() {
    int result = object.hashCode();
    result = 31 * result + (int)(version ^ (version >>> 32));
    return result;
  }
}
