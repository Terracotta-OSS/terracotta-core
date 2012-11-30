/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import com.google.common.base.Preconditions;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class ServerMapKeySet<K, V> extends AbstractSet<K> {

  private final ServerMap<K, V> map;
  private final Set<K>          delegateKeySet;

  public ServerMapKeySet(ServerMap<K, V> clusteredMap, final Set<K> delegate) {
    map = clusteredMap;
    this.delegateKeySet = delegate;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(final Object key) {
    return map.containsKey(key);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean remove(final Object key) {
    return map.remove(key) != null;
  }

  @Override
  public Iterator<K> iterator() {
    return new KeyIterator<K, V>(map, this.delegateKeySet.iterator());
  }

  private class KeyIterator<KI, VI> implements Iterator<KI> {

    private final ServerMap<K, V> clusteredMap;
    private final Iterator<KI>    delegate;
    private KI                    lastKey;

    public KeyIterator(ServerMap<K, V> clusteredMap, final Iterator<KI> delegate) {
      this.clusteredMap = clusteredMap;
      this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
      return this.delegate.hasNext();
    }

    @Override
    public KI next() {
      final KI result = this.delegate.next();
      this.lastKey = result;
      return result;
    }

    @Override
    public void remove() {
      Preconditions.checkState(null != this.lastKey, "next needs to be called before calling remove");
      clusteredMap.remove(this.lastKey);
      this.lastKey = null;
    }

  }

}
