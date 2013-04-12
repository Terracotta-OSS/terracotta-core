/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.collections.ToolkitSortedMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ToolkitSortedMapImpl<K extends Comparable<? super K>, V> extends ToolkitMapImpl<K, V> implements
    ToolkitSortedMap<K, V> {
  private final SortedKeyValueHolder<K, V> sortedKeyValueHolder;

  public ToolkitSortedMapImpl() {
    super(new SortedKeyValueHolder(new ConcurrentSkipListMap<K, V>()));
    this.sortedKeyValueHolder = (SortedKeyValueHolder<K, V>) keyValueHolder;
  }

  @Override
  public Comparator<? super K> comparator() {
    return null;
  }

  @Override
  public K firstKey() {
    lock.readLock().lock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return sortedKeyValueHolder.firstKey();
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    lock.readLock().lock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return new RangeSortedMap(sortedKeyValueHolder.headMap(toKey), null, toKey);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public K lastKey() {
    lock.readLock().lock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return sortedKeyValueHolder.lastKey();
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    lock.readLock().lock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return new RangeSortedMap(sortedKeyValueHolder.subMap(fromKey, toKey), fromKey, toKey);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    lock.readLock().lock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return new RangeSortedMap(sortedKeyValueHolder.tailMap(fromKey), fromKey, null);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  private class RangeSortedMap implements SortedMap<K, V> {
    private final SortedMap<K, V> localMap;
    private final K               from;
    private final K               to;

    public RangeSortedMap(SortedMap<K, V> localMap, K from, K to) {
      this.localMap = localMap;
      this.from = from;
      this.to = to;
    }

    private void checkRange(Object k) {
      if (greaterThanOrEqual(k) && lessThan(k)) { return; }

      throw new IllegalArgumentException();
    }

    private boolean lessThan(Object k) {
      if (to == null) { return true; }

      return ((Comparable) to).compareTo(k) > 0;
    }

    private boolean greaterThanOrEqual(Object k) {
      if (from == null) { return true; }

      return (((Comparable) from).compareTo(k) <= 0);
    }

    @Override
    public void clear() {
      lock.writeLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          for (Object obj : new HashSet(localMap.keySet())) {
            ToolkitSortedMapImpl.this.remove(obj);
          }
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    @Override
    public boolean containsKey(Object key) {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return this.localMap.containsKey(key);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public boolean containsValue(Object value) {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return this.localMap.containsValue(value);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public V get(Object key) {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return this.localMap.get(key);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public boolean isEmpty() {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return this.localMap.isEmpty();
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public V put(K key, V value) {
      checkRange(key);

      lock.writeLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return ToolkitSortedMapImpl.this.put(key, value);
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      for (K k : m.keySet()) {
        checkRange(k);
      }

      lock.writeLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          ToolkitSortedMapImpl.this.putAll(m);
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    @Override
    public V remove(Object key) {
      checkRange(key);

      lock.writeLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          if (!localMap.containsKey(key)) { return null; }
          return unlockedRemove(key);
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    @Override
    public int size() {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localMap.size();
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public Comparator<? super K> comparator() {
      return null;
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new RangeToolkitMapEntrySet(this.localMap.entrySet(), this);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public K firstKey() {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return this.localMap.firstKey();
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new RangeSortedMap(localMap.headMap(toKey), from, toKey);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public Set<K> keySet() {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new RangeToolkitKeySet(localMap.keySet(), this);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public K lastKey() {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localMap.lastKey();
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new RangeSortedMap(localMap.subMap(fromKey, toKey), fromKey, toKey);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new RangeSortedMap(localMap.tailMap(fromKey), fromKey, to);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

    @Override
    public Collection<V> values() {
      lock.readLock().lock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return new ToolkitValueCollection(localMap);
        }
      } finally {
        lock.readLock().unlock();
      }
    }

  }

  private static class SortedKeyValueHolder<K, V> extends KeyValueHolder<K, V> {
    private final SortedMap<K, V> sortedMap;

    public SortedKeyValueHolder(SortedMap<K, V> sortedMap) {
      super(sortedMap);
      this.sortedMap = sortedMap;
    }

    public K lastKey() {
      return sortedMap.lastKey();
    }

    public SortedMap<K, V> headMap(K toKey) {
      return sortedMap.headMap(toKey);
    }

    public K firstKey() {
      return sortedMap.firstKey();
    }

    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return sortedMap.subMap(fromKey, toKey);
    }

    public SortedMap<K, V> tailMap(K fromKey) {
      return sortedMap.tailMap(fromKey);
    }
  }

  private class RangeToolkitMapEntrySet extends ToolkitMapEntrySet {
    private final RangeSortedMap internalSortedMap;

    public RangeToolkitMapEntrySet(Set<Entry<K, V>> entrySet, RangeSortedMap sortedMap) {
      super(entrySet);
      this.internalSortedMap = sortedMap;
    }

    @Override
    public boolean add(java.util.Map.Entry<K, V> e) {
      internalSortedMap.checkRange(e.getKey());

      return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends Entry<K, V>> c) {
      for (Entry<K, V> e : c) {
        internalSortedMap.checkRange(e.getKey());
      }

      return super.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
      internalSortedMap.checkRange(((Entry) o).getKey());

      return super.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      for (Object e : c) {
        internalSortedMap.checkRange(((Entry) e).getKey());
      }
      return super.removeAll(c);
    }

  }

  private class RangeToolkitKeySet extends ToolkitKeySet {
    private final RangeSortedMap internalSortedMap;

    public RangeToolkitKeySet(Set<K> keySet, RangeSortedMap sortedMap) {
      super(keySet);
      this.internalSortedMap = sortedMap;
    }

    @Override
    public boolean add(K e) {
      internalSortedMap.checkRange(e);

      return super.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
      for (K e : c) {
        internalSortedMap.checkRange(e);
      }

      return super.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
      internalSortedMap.checkRange(o);

      return super.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      for (Object e : c) {
        internalSortedMap.checkRange(e);
      }
      return super.removeAll(c);
    }

  }
}
