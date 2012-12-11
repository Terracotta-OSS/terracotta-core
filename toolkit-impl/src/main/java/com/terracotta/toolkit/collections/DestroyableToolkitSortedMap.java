/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.collections.map.ToolkitSortedMapImpl;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitMap;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public class DestroyableToolkitSortedMap<K extends Comparable<? super K>, V> extends
    AbstractDestroyableToolkitObject<ToolkitSortedMap> implements ToolkitSortedMap<K, V>, RejoinAwareToolkitMap<K, V> {

  private final String                                              name;
  private volatile ToolkitSortedMap<K, V>                           map;
  private final IsolatedClusteredObjectLookup<ToolkitSortedMapImpl> lookup;
  private volatile int                                              currentRejoinCount;

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
    currentRejoinCount++;
  }

  @Override
  public void rejoinCompleted() {
    ToolkitSortedMapImpl afterRejoin = lookup.lookupClusteredObject(name);
    if (afterRejoin != null) {
      this.map = afterRejoin;
    } else {
      // didn't find backing clustered object after rejoin - must have been destroyed
      // apply destory locally
      applyDestroy();
    }
  }

  @Override
  public void applyDestroy() {
    this.map = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitSortedMap.class);
  }

  @Override
  public void doDestroy() {
    map.destroy();
  }

  private void exceptionIfDestroyedOrRejoined(int expectedRejoinCount) {
    if (isDestroyed()) { throw new IllegalStateException("This object has already been destroyed"); }
    if (expectedRejoinCount != DestroyableToolkitSortedMap.this.currentRejoinCount) { throw new RejoinException(
                                                                                                                "This SubType is not usable anymore after the rejoin has occured!"); }
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

  private class DestroyableSet extends DestroyableCollection implements Set {
    public DestroyableSet(Set set) {
      super(set);
    }
  }

  private class DestroyableCollection implements Collection {
    private final Collection collection;
    private final int        rejoinCount;

    public DestroyableCollection(Collection collection) {
      this.collection = collection;
      this.rejoinCount = DestroyableToolkitSortedMap.this.currentRejoinCount;
    }

    @Override
    public int size() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.size();
    }

    @Override
    public boolean isEmpty() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.contains(o);
    }

    @Override
    public Iterator iterator() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return new DestroyableIterator(collection.iterator(), DestroyableToolkitSortedMap.this);
    }

    @Override
    public Object[] toArray() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.toArray(a);
    }

    @Override
    public boolean add(Object e) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.add(e);
    }

    @Override
    public boolean remove(Object o) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.remove(o);
    }

    @Override
    public boolean containsAll(Collection c) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.containsAll(c);
    }

    @Override
    public boolean addAll(Collection c) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.addAll(c);
    }

    @Override
    public boolean removeAll(Collection c) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection c) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return collection.retainAll(c);
    }

    @Override
    public void clear() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
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
    private final int             rejoinCount;

    public DestroyableSortedMap(SortedMap<K, V> innerMap) {
      this.sortedMap = innerMap;
      this.rejoinCount = DestroyableToolkitSortedMap.this.currentRejoinCount;
    }

    @Override
    public void clear() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      sortedMap.clear();
    }

    @Override
    public boolean containsKey(Object key) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.get(key);
    }

    @Override
    public boolean isEmpty() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.isEmpty();
    }

    @Override
    public V put(K key, V value) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      sortedMap.putAll(m);
    }

    @Override
    public V remove(Object key) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.remove(key);
    }

    @Override
    public int size() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.size();
    }

    @Override
    public Comparator<? super K> comparator() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return null;
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return new DestroyableSet(sortedMap.entrySet());
    }

    @Override
    public K firstKey() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.firstKey();
    }

    @Override
    public SortedMap<K, V> headMap(K toKey) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return new DestroyableSortedMap(sortedMap.headMap(toKey));
    }

    @Override
    public Set<K> keySet() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return new DestroyableSet(sortedMap.keySet());
    }

    @Override
    public K lastKey() {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return sortedMap.lastKey();
    }

    @Override
    public SortedMap<K, V> subMap(K fromKey, K toKey) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return new DestroyableSortedMap(sortedMap.subMap(fromKey, toKey));
    }

    @Override
    public SortedMap<K, V> tailMap(K fromKey) {
      exceptionIfDestroyedOrRejoined(this.rejoinCount);
      return new DestroyableSortedMap(sortedMap.tailMap(fromKey));
    }

    @Override
    public Collection<V> values() {
      return new DestroyableCollection(map.values());
    }

  }
}
