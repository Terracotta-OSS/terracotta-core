/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

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

}
