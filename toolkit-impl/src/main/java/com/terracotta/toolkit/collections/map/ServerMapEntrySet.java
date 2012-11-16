/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

public class ServerMapEntrySet<K, V> extends AbstractSet<Entry<K, V>> {

  private final ServerMap<K, V> map;
  private final Set<K>          keys;

  public ServerMapEntrySet(ServerMap<K, V> clusteredMap, final Set<K> keys) {
    this.map = clusteredMap;
    this.keys = keys;
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return new EntryIterator<K, V>(map, this.keys.iterator());
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(final Object o) {
    if (!(o instanceof Entry)) { return false; }
    final Entry e = (Entry) o;
    final V value = map.get(e.getKey());
    return value != null && value.equals(e.getValue());
  }

  @Override
  public boolean remove(final Object o) {
    if (!(o instanceof Entry)) { return false; }
    final Entry e = (Entry) o;
    return map.remove(e.getKey(), e.getValue());
  }

  @Override
  public void clear() {
    map.clear();
  }

  private static class EntryIterator<T, D> implements Iterator<Entry<T, D>> {
    /**
     *
     */
    private final ServerMap<T, D> map;
    private final Iterator<T>     delegateKeySet;
    private Entry<T, D>           nextEntry;
    private Entry<T, D>           currentEntry;

    public EntryIterator(ServerMap<T, D> clusteredMap, final Iterator<T> delegateKeySet) {
      this.map = clusteredMap;
      this.delegateKeySet = delegateKeySet;
      advance();
    }

    private void advance() {
      nextEntry = null;

      while (delegateKeySet.hasNext()) {
        T key = delegateKeySet.next();
        D value = map.get(key);
        if (value == null) {
          continue;
        }
        nextEntry = new ServerMapEntry<T, D>(map, key, value);
        break;
      }
    }

    @Override
    public synchronized boolean hasNext() {
      return nextEntry != null;
    }

    @Override
    public synchronized Entry<T, D> next() {
      if (nextEntry == null) throw new NoSuchElementException();

      currentEntry = nextEntry;
      advance();
      return currentEntry;
    }

    @Override
    public synchronized void remove() {
      if (null == this.currentEntry) { throw new IllegalStateException("next needs to be called before calling remove"); }

      map.remove(this.currentEntry.getKey(), this.currentEntry.getValue());
      this.currentEntry = null;
    }
  }

  private static class ServerMapEntry<B, C> implements Entry<B, C> {

    private final ServerMap<B, C> map;
    private final B               key;
    private C                     value;

    private ServerMapEntry(ServerMap<B, C> clusteredMap, final B key, final C value) {
      map = clusteredMap;
      this.key = key;
      this.value = value;
    }

    @Override
    public B getKey() {
      // entry locking here doesn't make sense since
      // 1. Entry keys can't change
      // 2. you're have to get the key anyway to generate the lock ID
      return this.key;
    }

    @Override
    public C getValue() {
      return this.value;
    }

    @Override
    public C setValue(final C newValue) {
      final C old = map.put(this.key, newValue);
      this.value = newValue;
      return old;
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Map.Entry)) { return false; }
      final Map.Entry e = (Map.Entry) o;
      return eq(this.key, e.getKey()) && eq(getValue(), e.getValue());
    }

    @Override
    public int hashCode() {
      return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value == null ? 0 : this.value.hashCode());
    }

    @Override
    public String toString() {
      return this.key + "=" + getValue();
    }

    private boolean eq(final Object o1, final Object o2) {
      return o1 == null ? o2 == null : o1.equals(o2);
    }

  }
}
