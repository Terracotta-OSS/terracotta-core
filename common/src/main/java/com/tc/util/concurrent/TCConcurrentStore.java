/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class provides basic map operations like put,get, remove with the specified amount of concurrency. It doesn't do
 * various optimizations like {@link java.util.concurrent.ConcurrentHashMap} on reads so get might be slower than CHM
 * but is still striped so faster than Hashtable for concurrent use cases.
 * <p>
 * Where this class will excel is when you want to perform certain operation (using the callback) with in the lock for
 * that segment. For example, creating a MultiMap becomes as simple as
 * <p>
 * <hr>
 * <blockquote>
 * 
 * <pre>
 * // Put into MultiMap
 * tcConcurrentStore.executeUnderWriteLock(key, value, new TCConcurrentStoreCallback() {
 *   public Object callback(Object key, Object value, Map segment) {
 *     boolean newEntry = false;
 *     List list = segment.get(key);
 *     if (list == null) {
 *       list = new ArrayList();
 *       segment.put(key, list);
 *       newEntry = true;
 *     }
 *     list.add(value);
 *     return newEntry;
 *   }
 * });
 * 
 * // Get From MultiMap
 * tcConcurrentStore.remove(key);
 * 
 * </pre>
 * 
 * </blockquote>
 * <p>
 * Someday this class could implement all the methods of {@link java.util.concurrent.ConcurrentMap}
 * <hr>
 * 
 * @author Saravanan Subbiah
 */
public class TCConcurrentStore<K, V> implements PrettyPrintable {

  static final int              MAX_SEGMENTS             = 1 << 16;
  static final int              MAXIMUM_CAPACITY         = 1 << 30;
  static final float            DEFAULT_LOAD_FACTOR      = 0.75f;
  static final int              DEFAULT_INITIAL_CAPACITY = 256;
  static final int              DEFAULT_SEGMENTS         = 16;

  private final int             segmentShift;
  private final int             segmentMask;

  private final Segment<K, V>[] segments;

  /**
   * Creates a store with a default initial capacity (16), load factor (0.75) and concurrencyLevel (16).
   */
  public TCConcurrentStore() {
    this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENTS);
  }

  /**
   * Creates a store with the specified initial capacity, and with default load factor (0.75) and concurrencyLevel (16).
   * 
   * @param initialCapacity the initial capacity. The implementation performs internal sizing to accommodate this many
   *        elements.
   * @throws IllegalArgumentException if the initial capacity of elements is negative.
   */
  public TCConcurrentStore(final int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENTS);
  }

  /**
   * Creates a store with the specified initial capacity and load factor and with the default concurrencyLevel (16).
   * 
   * @param initialCapacity The implementation performs internal sizing to accommodate this many elements.
   * @param loadFactor the load factor threshold, used to control resizing. Resizing may be performed when the average
   *        number of elements per bin exceeds this threshold.
   * @throws IllegalArgumentException if the initial capacity of elements is negative or the load factor is non-positive
   */
  public TCConcurrentStore(final int initialCapacity, final float loadFactor) {
    this(initialCapacity, loadFactor, DEFAULT_SEGMENTS);
  }

  /**
   * Creates a store with the specified initial capacity, load factor and concurrency level.
   * 
   * @param initialCapacity the initial capacity. The implementation performs internal sizing to accommodate this many
   *        elements.
   * @param loadFactor the load factor threshold, used to control resizing. Resizing may be performed when the average
   *        number of elements per bin exceeds this threshold.
   * @param concurrencyLevel the estimated number of concurrently updating threads. The implementation performs internal
   *        sizing to try to accommodate this many threads.
   * @throws IllegalArgumentException if the initial capacity is negative or the load factor or concurrencyLevel are
   *         non-positive.
   */
  public TCConcurrentStore(int initialCapacity, final float loadFactor, int concurrencyLevel) {
    if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0) { throw new IllegalArgumentException(); }

    if (concurrencyLevel > MAX_SEGMENTS) {
      concurrencyLevel = MAX_SEGMENTS;
    }

    // Find power-of-two sizes best matching arguments
    int sshift = 0;
    int ssize = 1;
    while (ssize < concurrencyLevel) {
      ++sshift;
      ssize <<= 1;
    }
    this.segmentShift = 32 - sshift;
    this.segmentMask = ssize - 1;

    this.segments = new Segment[ssize];

    if (initialCapacity > MAXIMUM_CAPACITY) {
      initialCapacity = MAXIMUM_CAPACITY;
    }
    int c = initialCapacity / ssize;
    if (c * ssize < initialCapacity) {
      ++c;
    }
    int cap = 1;
    while (cap < c) {
      cap <<= 1;
    }

    for (int i = 0; i < this.segments.length; ++i) {
      this.segments[i] = new Segment<K, V>(cap, loadFactor);
    }
  }

  /**
   * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions. This
   * is critical because CachedItemStore uses power-of-two length hash tables, that otherwise encounter collisions for
   * hashCodes that do not differ in lower or upper bits.
   */
  private static int hash(int h) {
    // Spread bits to regularize both segment and index locations,
    // using variant of single-word Wang/Jenkins hash.
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  /**
   * Returns the segment that should be used for key with given hash
   * 
   * @param hash the hash code for the key
   * @return the segment
   */
  final Segment<K, V> segmentFor(final Object key) {
    final int hash = hash(key.hashCode()); // throws NullPointerException if key null
    return this.segments[(hash >>> this.segmentShift) & this.segmentMask];
  }

  /**
   * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the
   * key.
   * <p>
   * More formally, if this store contains a mapping from a key {@code k} to a value {@code v} such that
   * {@code key.equals(k)}, then this method returns {@code v}; otherwise it returns {@code null}. (There can be at most
   * one such mapping.)
   * 
   * @throws NullPointerException if the specified key is null
   */
  public V get(final K key) {
    return segmentFor(key).get(key);
  }

  /**
   * Maps the specified key to the specified value in this table. Neither the key nor the value can be null.
   * <p>
   * The value can be retrieved by calling the <tt>get</tt> method with a key that is equal to the original key.
   * 
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for <tt>key</tt>
   * @throws NullPointerException if the specified key or value is null
   */
  public V put(final K key, final V value) {
    if (value == null) { throw new NullPointerException(); }
    return segmentFor(key).put(key, value);
  }

  /**
   * If the specified key is not already associated with a value, associate it with the given value. This is equivalent
   * to
   * 
   * <pre>
   * if (!map.containsKey(key)) return map.put(key, value);
   * else return map.get(key);
   * </pre>
   * 
   * except that the action is performed atomically.
   * 
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with the specified key, or <tt>null</tt> if there was no mapping for the key
   * @throws NullPointerException if the specified key or value is null
   */
  public V putIfAbsent(final K key, final V value) {
    if (value == null) { throw new NullPointerException(); }
    return segmentFor(key).putIfAbsent(key, value);
  }

  /**
   * Removes the key (and its corresponding value) from this map. This method does nothing if the key is not in the map.
   * 
   * @param key the key that needs to be removed
   * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no mapping for <tt>key</tt>
   * @throws NullPointerException if the specified key is null
   */
  public V remove(final K key) {
    return segmentFor(key).remove(key);
  }

  /**
   * Executes the callback under the read lock for the segment where the key could possibly be present.
   * 
   * @param key the key mapping to the segment
   * @param param any user-defined param
   * @param callback the callback that is executed
   * @return the return value from the callback function
   * @throws NullPointerException if the specified key is null
   */
  public Object executeUnderReadLock(final K key, final Object param, final TCConcurrentStoreCallback<K, V> callback) {
    return segmentFor(key).executeUnderReadLock(key, param, callback);
  }

  /**
   * Executes the callback under the write lock for the segment where the key could possibly be present.
   * 
   * @param key the key mapping to the segment
   * @param param any user-defined param
   * @param callback the callback that is executed
   * @return the return value from the callback function
   * @throws NullPointerException if the specified key is null
   */
  public Object executeUnderWriteLock(final K key, final Object param, final TCConcurrentStoreCallback<K, V> callback) {
    return segmentFor(key).executeUnderWriteLock(key, param, callback);
  }

  /**
   * Adds a snapshot of all the keys in the concurrent store to the set that is passed in. This method does not lock the
   * entire map so concurrent modifications are possible while this call is executing.
   * 
   * @param keySet the Set to add the keys to
   * @return the set that is passed in.
   */
  public Set addAllKeysTo(Set keySet) {
    for (Segment<K, V> seg : segments) {
      seg.addAllKeysTo(keySet);
    }
    return keySet;
  }

  /**
   * Returns the number of keys present across all segments. This method is fully locked and hence costly to call.
   * 
   * @return size
   */
  public int size() {
    fullyReadLock();
    try {
      int size = 0;
      for (Segment<K, V> seg : segments) {
        size += seg.size();
      }
      return size;
    } finally {
      fullyReadUnlock();
    }
  }

  private void fullyReadLock() {
    for (Segment<K, V> seg : segments) {
      seg.readLock().lock();
    }
  }

  private void fullyReadUnlock() {
    for (Segment<K, V> seg : segments) {
      seg.readLock().unlock();
    }
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    for (int i = 0; i < segments.length; i++) {
      out.duplicateAndIndent().indent().print("segment " + i + ":").flush();
      out.visit(this.segments[i]).flush();
    }
    return out;
  }

  /**
   * The callback interface that needs to be implemented so <code>executeUnderWriteLock</code> and
   * <code>executeUnderReadLock</code> can be called
   */
  public interface TCConcurrentStoreCallback<K, V> {

    public Object callback(K key, Object param, Map<K, V> segment);
  }

  private static final class Segment<K, V> extends ReentrantReadWriteLock implements PrettyPrintable {

    private final HashMap<K, V> map;

    public Segment(final int initialCapacity, final float loadFactor) {
      this.map = new HashMap<K, V>(initialCapacity, loadFactor);
    }

    public int size() {
      return map.size();
    }

    public Set addAllKeysTo(Set keySet) {
      this.readLock().lock();
      try {
        keySet.addAll(this.map.keySet());
        return keySet;
      } finally {
        this.readLock().unlock();
      }
    }

    public V get(final K key) {
      this.readLock().lock();
      try {
        return this.map.get(key);
      } finally {
        this.readLock().unlock();
      }
    }

    public V put(final K key, final V value) {
      this.writeLock().lock();
      try {
        return this.map.put(key, value);
      } finally {
        this.writeLock().unlock();
      }
    }

    public V putIfAbsent(final K key, final V value) {
      this.writeLock().lock();
      try {
        if (!this.map.containsKey(key)) {
          return this.map.put(key, value);
        } else {
          return this.map.get(key);
        }
      } finally {
        this.writeLock().unlock();
      }
    }

    public V remove(final K key) {
      this.writeLock().lock();
      try {
        return this.map.remove(key);
      } finally {
        this.writeLock().unlock();
      }
    }

    public Object executeUnderReadLock(final K key, final Object param, final TCConcurrentStoreCallback<K, V> callback) {
      this.readLock().lock();
      try {
        return callback.callback(key, param, this.map);
      } finally {
        this.readLock().unlock();
      }
    }

    public Object executeUnderWriteLock(final K key, final Object param, final TCConcurrentStoreCallback<K, V> callback) {
      this.writeLock().lock();
      try {
        return callback.callback(key, param, this.map);
      } finally {
        this.writeLock().unlock();
      }
    }

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      this.readLock().lock();
      try {
        Iterator<Map.Entry<K, V>> entries = this.map.entrySet().iterator();
        while (entries.hasNext()) {
          Map.Entry<K, V> e = entries.next();
          out.duplicateAndIndent().duplicateAndIndent().indent().print(e.getKey() + " => " + e.getValue()).flush();
        }
      } finally {
        this.readLock().unlock();
      }
      return out;
    }
  }
}
