/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class DestroyableToolkitSortedMap<K extends Comparable<? super K>, V> extends
    AbstractDestroyableToolkitObject<ToolkitSortedMap> implements ToolkitSortedMap<K, V>, RejoinAwareToolkitObject {

  private final String                                              name;
  private volatile ToolkitSortedMap<K, V>                           map;
  private final IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup;

  public DestroyableToolkitSortedMap(ToolkitObjectFactory<ToolkitSortedMap> factory,
                                     IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup,
                                     ToolkitSortedMapImpl<K, V> map, String name) {
    super(factory);
    this.lookup = lookup;
    this.map = map;
    this.name = name;
    map.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void rejoinStarted() {
    this.map = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitSortedMap.class);
  }

  @Override
  public void rejoinCompleted() {
    ToolkitSortedMapImpl afterRejoin = lookup.lookupClusteredObject(name);
    if (afterRejoin == null) {
      // didn't find backing clustered object after rejoin - must have been destroyed
      // todo: set to a new delegate which throws exception, as clustered object is destroyed
    }
    this.map = afterRejoin;
  }

  @Override
  public void applyDestroy() {
    this.map = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitSortedMap.class);
  }

  @Override
  public void doDestroy() {
    map.destroy();
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return map.getReadWriteLock();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return map.get(key);
  }

  @Override
  public V put(K key, V value) {
    return map.put(key, value);
  }

  @Override
  public V remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    map.putAll(m);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<K> keySet() {
    return new DestroyableSet(map.keySet());
  }

  @Override
  public Collection<V> values() {
    return new DestroyableCollection(map.values());
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return new DestroyableSet(map.entrySet());
  }

  private void exceptionIfDestroyed() {
    if (isDestroyed()) { throw new IllegalStateException("This object has already been destroyed"); }
  }

  private class DestroyableSet extends DestroyableCollection implements Set {
    public DestroyableSet(Set set) {
      super(set);
    }
  }

  private class DestroyableCollection implements Collection {
    private final Collection collection;

    public DestroyableCollection(Collection collection) {
      this.collection = collection;
    }

    @Override
    public int size() {
      exceptionIfDestroyed();
      return collection.size();
    }

    @Override
    public boolean isEmpty() {
      exceptionIfDestroyed();
      return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      exceptionIfDestroyed();
      return collection.contains(o);
    }

    @Override
    public Iterator iterator() {
      exceptionIfDestroyed();
      return new DestroyableIterator(collection.iterator(), DestroyableToolkitSortedMap.this);
    }

    @Override
    public Object[] toArray() {
      exceptionIfDestroyed();
      return collection.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
      exceptionIfDestroyed();
      return collection.toArray(a);
    }

    @Override
    public boolean add(Object e) {
      exceptionIfDestroyed();
      return collection.add(e);
    }

    @Override
    public boolean remove(Object o) {
      exceptionIfDestroyed();
      return collection.remove(o);
    }

    @Override
    public boolean containsAll(Collection c) {
      exceptionIfDestroyed();
      return collection.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
      exceptionIfDestroyed();
      return collection.addAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
      exceptionIfDestroyed();
      return collection.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
      exceptionIfDestroyed();
      return collection.retainAll(c);
    }

    @Override
    public void clear() {
      exceptionIfDestroyed();
      collection.clear();
    }
  }

  @Override
  public Comparator<? super K> comparator() {
    return null;
  }

  @Override
  public K firstKey() {
    return map.firstKey();
  }

  @Override
  public K lastKey() {
    return map.lastKey();
  }

  @Override
  public SortedMap<K, V> headMap(K toKey) {
    return new DestroyableSortedMap(map.headMap(toKey));
  }

  @Override
  public SortedMap<K, V> subMap(K fromKey, K toKey) {
    return new DestroyableSortedMap(map.subMap(fromKey, toKey));
  }

  @Override
  public SortedMap<K, V> tailMap(K fromKey) {
    return new DestroyableSortedMap(map.tailMap(fromKey));
  }

  private class DestroyableSortedMap implements SortedMap<K, V> {
    private final SortedMap<K, V> sortedMap;

    public DestroyableSortedMap(SortedMap<K, V> innerMap) {
      this.sortedMap = innerMap;
    }

    @Override
    public void clear() {
      exceptionIfDestroyed();
      sortedMap.clear();
    }

    @Override
    public boolean containsKey(Object key) {
      exceptionIfDestroyed();
      return sortedMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      exceptionIfDestroyed();
      return sortedMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
      exceptionIfDestroyed();
      return sortedMap.get(key);
    }

    @Override
    public boolean isEmpty() {
      exceptionIfDestroyed();
      return sortedMap.isEmpty();
    }

    @Override
    public V put(K key, V value) {
      exceptionIfDestroyed();
      return sortedMap.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      exceptionIfDestroyed();
      sortedMap.putAll(m);
    }

    @Override
    public V remove(Object key) {
      exceptionIfDestroyed();
      return sortedMap.remove(key);
    }

    @Override
    public int size() {
      exceptionIfDestroyed();
      return sortedMap.size();
    }

    @Override
    public Comparator<? super K> comparator() {
      exceptionIfDestroyed();
      return null;
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
      exceptionIfDestroyed();
      return new DestroyableSet(sortedMap.entrySet());
    }

    @Override
    public K firstKey() {
      exceptionIfDestroyed();
      return sortedMap.firstKey();
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      exceptionIfDestroyed();
      return new DestroyableSortedMap(sortedMap.headMap(toKey));
    }

    @Override
    public Set<K> keySet() {
      exceptionIfDestroyed();
      return new DestroyableSet(sortedMap.keySet());
    }

    @Override
    public K lastKey() {
      exceptionIfDestroyed();
      return sortedMap.lastKey();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      exceptionIfDestroyed();
      return new DestroyableSortedMap(sortedMap.subMap(fromKey, toKey));
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      exceptionIfDestroyed();
      return new DestroyableSortedMap(sortedMap.tailMap(fromKey));
    }

    @Override
    public Collection<V> values() {
      return new DestroyableCollection(map.values());
    }

  }
}
