package com.tc.gbapi;

import java.util.Collection;
import java.util.Set;

/**
 * @author tim
 */
public interface GBMap<K, V> {

  // We can probably get rid of these
  @Deprecated
  public Set<K> keySet(); // only used for loading root mbeans

  @Deprecated
  public Collection<V> values(); // only used for retrieving GC roots,
  // maybe we can just make the entire root
  // map a single value to avoid this.

  @Deprecated
  public long size(); // only really used for stats logging

  // TODO: what exception to catch when
  public void put(K key, V value);

  public V get(K key);

  public boolean remove(K key);

  public void removeAll(Collection<K> keys);

  public boolean containsKey(K key);

  public void clear();
}
