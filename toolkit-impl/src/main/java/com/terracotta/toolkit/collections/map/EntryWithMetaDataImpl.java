/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata.EntryWithMetaData;
import org.terracotta.toolkit.internal.meta.MetaData;

public class EntryWithMetaDataImpl<K, V> implements EntryWithMetaData<K, V> {

  private final K        key;
  private final V        value;
  private final MetaData metaData;

  public EntryWithMetaDataImpl(K key, V value, MetaData metaData) {
    this.key = key;
    this.value = value;
    this.metaData = metaData;
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  public MetaData getMetaData() {
    return metaData;
  }

  @Override
  public V setValue(V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((metaData == null) ? 0 : metaData.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EntryWithMetaDataImpl other = (EntryWithMetaDataImpl) obj;
    if (key == null) {
      if (other.key != null) return false;
    } else if (!key.equals(other.key)) return false;
    if (metaData == null) {
      if (other.metaData != null) return false;
    } else if (!metaData.equals(other.metaData)) return false;
    if (value == null) {
      if (other.value != null) return false;
    } else if (!value.equals(other.value)) return false;
    return true;
  }

}
