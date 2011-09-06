/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.concurrent.TCConcurrentStore.TCConcurrentStoreCallback;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A concurrent implementation of a MultiMap (one to many mapping) with configurable concurrency level. Basic methods
 * are implemented. Could one day implement all map interfaces.
 * 
 * @author Saravanan Subbiah
 */
public class TCConcurrentMultiMap<K, V> implements PrettyPrintable {

  private final AddCallBack<K, V>            addCallback    = new AddCallBack<K, V>();
  private final AddAllCallBack<K, V>         addAllCallback = new AddAllCallBack<K, V>();
  private final RemoveCallBack<K, V>         removeCallback = new RemoveCallBack<K, V>();

  private final TCConcurrentStore<K, Set<V>> store;

  /**
   * Creates a Multimap with a default initial capacity (16), load factor (0.75) and concurrencyLevel (16).
   */
  public TCConcurrentMultiMap() {
    this.store = new TCConcurrentStore<K, Set<V>>();
  }

  /**
   * Creates a Multimap with the specified initial capacity, and with default load factor (0.75) and concurrencyLevel
   * (16).
   * 
   * @param initialCapacity the initial capacity.
   * @throws IllegalArgumentException if the initial capacity of elements is negative.
   */
  public TCConcurrentMultiMap(final int initialCapacity) {
    this.store = new TCConcurrentStore<K, Set<V>>(initialCapacity);
  }

  /**
   * Creates a Multimap with the specified initial capacity and load factor and with the default concurrencyLevel (16).
   * 
   * @param initialCapacity the initial capacity.
   * @param loadFactor the load factor threshold, used to control resizing.
   * @throws IllegalArgumentException if the initial capacity of elements is negative or the load factor is non-positive
   */
  public TCConcurrentMultiMap(final int initialCapacity, final float loadFactor) {
    this.store = new TCConcurrentStore<K, Set<V>>(initialCapacity, loadFactor);
  }

  /**
   * Creates a Multimap with the specified initial capacity, load factor and concurrency level.
   * 
   * @param initialCapacity the initial capacity.
   * @param loadFactor the load factor threshold, used to control resizing.
   * @param concurrencyLevel the estimated number of concurrently updating threads.
   * @throws IllegalArgumentException if the initial capacity is negative or the load factor or concurrencyLevel are
   *         non-positive.
   */
  public TCConcurrentMultiMap(final int initialCapacity, final float loadFactor, final int concurrencyLevel) {
    this.store = new TCConcurrentStore<K, Set<V>>(initialCapacity, loadFactor, concurrencyLevel);
  }

  /**
   * Adds a mapping of key to value to the Multimap. If there already exists a mapping for the key, then the value is
   * added to the set of values mapped to that key. If there already exists a mapping for the key to the value, then the
   * Multimap is not mutated.
   * 
   * @return true, if this is the first mapping for key in this Multimap at this point in time, else false
   * @throws NullPointerException if key or value is null
   */
  public boolean add(final K key, final V value) {
    return (Boolean) this.store.executeUnderWriteLock(key, value, this.addCallback);
  }

  /**
   * Adds all mapping of key to Set of values to the Multimap. If there already exists a mapping for the key, then the
   * value is added to the set of values mapped to that key.
   * 
   * @return true, if there exists no mapping for this key before this call.
   * @throws NullPointerException if key or value is null
   */
  public boolean addAll(final K key, final Set<V> values) {
    return (Boolean) this.store.executeUnderWriteLock(key, values, this.addAllCallback);
  }

  /**
   * Removes the mapping of key to value if it exists in the Multimap.
   * 
   * @returns true if the mapping existed and was successfully removed, false if the mapping didn't exist.
   * @throws NullPointerException if key or value is null
   */
  public boolean remove(final K key, final V value) {
    return (Boolean) this.store.executeUnderWriteLock(key, value, this.removeCallback);
  }

  /**
   * Removes all the mapping for the key and returns as a List. If there are no mapping present for the key, returns an
   * empty list.
   * 
   * @return list of mappings for key
   * @throws NullPointerException if key is null
   */
  public Set<V> removeAll(final K key) {
    final Set<V> set = this.store.remove(key);
    if (set == null) { return Collections.EMPTY_SET; }
    return set;
  }

  /**
   * Returns all the mapping for the key as an immutable List. If there are no mapping present for the key, returns an
   * empty list. Note that even though the returned list is immutable, the list is backed by the mappings in the
   * Multimap, so iterating the returned list while there are concurrent operations for the same key will produce
   * undetermined results.
   * 
   * @return list of mappings for key
   * @throws NullPointerException if key is null
   */
  public Set<V> get(final K key) {
    final Set<V> set = this.store.get(key);
    if (set == null) { return Collections.EMPTY_SET; }
    return Collections.unmodifiableSet(set);
  }

  /**
   * Checks the presence of some mapping for the key.
   * 
   * @return true is a mapping exists, false otherwise
   * @throws NullPointerException if key is null
   */
  public boolean containsKey(final K key) {
    final Set<V> set = this.store.get(key);
    if (set == null) { return false; }
    return true;
  }

  /**
   * Returns the number of keys present across all segments. This method is fully locked and hence costly to call.
   * 
   * @return size
   */
  public int size() {
    return store.size();
  }

  private static class AddCallBack<K, V> implements TCConcurrentStoreCallback<K, Set<V>> {
    // Called under segment lock
    public Object callback(final K key, final Object value, final Map<K, Set<V>> segment) {
      boolean newEntry = false;
      Set<V> set = segment.get(key);
      if (set == null) {
        set = new HashSet<V>();
        segment.put(key, set);
        newEntry = true;
      }
      set.add((V) value);
      return newEntry;
    }
  }

  private static final class AddAllCallBack<K, V> implements TCConcurrentStoreCallback<K, Set<V>> {
    // Called under segment lock
    public Object callback(final K key, final Object values, final Map<K, Set<V>> segment) {
      boolean newEntry = false;
      Set<V> set = segment.get(key);
      if (set == null) {
        set = new HashSet<V>();
        segment.put(key, set);
        newEntry = true;
      }
      Set<V> values2Add = (Set<V>) values;
      set.addAll(values2Add);
      return newEntry;
    }
  }

  private static final class RemoveCallBack<K, V> implements TCConcurrentStoreCallback<K, Set<V>> {
    // Called under segment lock
    public Object callback(final K key, final Object value, final Map<K, Set<V>> segment) {
      final Set<V> set = segment.get(key);
      if (set == null) { return false; }
      final boolean removed = set.remove(value);
      if (set.isEmpty()) {
        segment.remove(key);
      }
      return removed;
    }
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.visit(this.store).flush();
    return out;
  }

}
