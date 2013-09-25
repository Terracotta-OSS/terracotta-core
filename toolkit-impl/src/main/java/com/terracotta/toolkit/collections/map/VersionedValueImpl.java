/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.internal.cache.VersionedValue;

public class VersionedValueImpl<V> implements VersionedValue<V> {

  private final V    value;
  private final long version;

  public VersionedValueImpl(V value, long version) {
    this.value = value;
    this.version = version;
  }

  @Override
  public V getValue() {
    return this.value;
  }

  @Override
  public long getVersion() {
    return this.version;
  }

}
