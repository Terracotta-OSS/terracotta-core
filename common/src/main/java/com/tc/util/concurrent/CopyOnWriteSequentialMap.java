/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map analogy of {@link java.util.concurrent.CopyOnWriteArraySet}; it provides a thread-safe variant of
 * {@link java.util.LinkedHashMap} by caching contents of decorated <tt>Map</tt> as a fully detached snapshot, updated
 * on each mutation. Thus, all write operations on this class are write-through, while read access is from the snapshot
 * only.
 * <p>
 * This map is best suited for applications in which map sizes generally stay small, read-only operations vastly
 * outnumber mutative operations, and you need to prevent interference among threads during traversal while avoiding
 * synchronization. It also maintains insertion order of its elements for iteration purposes.
 * <p>
 * Iterators returned from results of calls to {@link #keySet()}, {@link #entrySet()} and {@link #values()} are
 * "snapshot" style: they reflect the state of the map at the point that the iterator was created. This map never
 * changes during the lifetime of the iterator, so interference is impossible and the iterator is guaranteed not to
 * throw <tt>ConcurrentModificationException</tt>. The iterator will not reflect additions, removals, or changes to the
 * list since the iterator was created. Element-changing operations on iterators themselves (<tt>remove</tt>,
 * <tt>set</tt>, and <tt>add</tt>) are not supported. These methods throw <tt>UnsupportedOperationException</tt>.
 */
public class CopyOnWriteSequentialMap<K, V> extends LinkedHashMap<K, V> {

  public interface TypedArrayFactory {
    public <R> R[] createTypedArray(int size);
  }

  private volatile Map<K, V> _snapshot;
  private final TypedArrayFactory _factory;
  private boolean                        inPutAll                    = false;

  private final static TypedArrayFactory DEFAULT_TYPED_ARRAY_FACTORY = new TypedArrayFactory() {

                                                                       @Override
                                                                       public Object[] createTypedArray(int size) {
                                                                         return new Object[size];
                                                                       }
                                                                     };

  public CopyOnWriteSequentialMap(int initialCapacity, float loadFactor) {
    this(initialCapacity, loadFactor, DEFAULT_TYPED_ARRAY_FACTORY);
  }

  public CopyOnWriteSequentialMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_TYPED_ARRAY_FACTORY);
  }

  public CopyOnWriteSequentialMap() {
    this(DEFAULT_TYPED_ARRAY_FACTORY);
  }

  public CopyOnWriteSequentialMap(Map<? extends K, ? extends V> m) {
    this(m, DEFAULT_TYPED_ARRAY_FACTORY);
  }

  public CopyOnWriteSequentialMap(int initialCapacity, float loadFactor, TypedArrayFactory f) {
    super(initialCapacity, loadFactor);
    _factory = f;
    takeSnapshot();
  }

  public CopyOnWriteSequentialMap(int initialCapacity, TypedArrayFactory f) {
    super(initialCapacity);
    _factory = f;
    takeSnapshot();
  }

  public CopyOnWriteSequentialMap(TypedArrayFactory f) {
    _factory = f;
    takeSnapshot();
  }

  public CopyOnWriteSequentialMap(Map<? extends K, ? extends V> m, TypedArrayFactory f) {
    super(m);
    _factory = f;
    takeSnapshot();
  }

  @Override
  public synchronized void clear() {
    super.clear();
    takeSnapshot();
  }

  /**
   * @return read-only entry set of underlying map's snapshot
   */
  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return _snapshot.entrySet();
  }

  /**
   * @return read-only key set of underlying map's snapshot
   */
  @Override
  public Set<K> keySet() {
    return _snapshot.keySet();
  }

  @Override
  public synchronized V put(K key, V value) {
    V old = super.put(key, value);
    takeSnapshot();
    return old;
  }

  @Override
  public synchronized V remove(Object key) {
    V old = super.remove(key);
    takeSnapshot();
    return old;
  }

  /**
   * @return read-only collection containing values of underlying map's snapshot
   */
  @Override
  public Collection<V> values() {
    return _snapshot.values();
  }

  public <R> R[] valuesToArray() {
    Collection<V> values = values();
    // Avoid "holes" in target array
    return values.toArray((R[]) _factory.createTypedArray(values.size()));
  }

  @Override
  public int size() {
    return _snapshot.size();
  }

  @Override
  public boolean isEmpty() {
    return _snapshot.isEmpty();
  }

  @Override
  public V get(Object key) {
    return _snapshot.get(key);
  }

  @Override
  public boolean containsKey(Object key) {
    return _snapshot.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return _snapshot.containsValue(value);
  }

  @Override
  public boolean equals(Object o) {
    return _snapshot.equals(o);
  }

  @Override
  public int hashCode() {
    return _snapshot.hashCode();
  }

  @Override
  public synchronized void putAll(Map<? extends K, ? extends V> m) {
    inPutAll = true;
    try {
      super.putAll(m);
    } finally {
      inPutAll = false;
      takeSnapshot();
    }
  }

  private void takeSnapshot() {
    // Defer taking snapshot until putAll() is finished. Needed because super.putAll() does a bunch of put()'s
    if (inPutAll) return;
    Map<K, V> temp = new LinkedHashMap<K, V>();
    for (Map.Entry<K, V> e : super.entrySet())
      temp.put(e.getKey(), e.getValue());
    _snapshot = Collections.unmodifiableMap(temp);
  }


}
