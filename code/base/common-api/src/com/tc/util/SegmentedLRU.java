package com.tc.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SegmentedLRU<K, V> {

  private static final int  DEFAULT_NUMBER_OF_SEGMENTS = 32;

  private final Map<K, V>[] segments;
  private final int         segmentShift;
  private final int         segmentMask;

  public SegmentedLRU(int capacity) {
    this(capacity, DEFAULT_NUMBER_OF_SEGMENTS);
  }

  public SegmentedLRU(int capacity, int numberOfSegments) {

    if (numberOfSegments < 2) { throw new IllegalArgumentException("Segment size should be 2 or greater"); }

    int sshift = 0;
    int ssize = 1;
    while (ssize < numberOfSegments) {
      ++sshift;
      ssize <<= 1;
    }
    this.segmentShift = 32 - sshift;
    this.segmentMask = ssize - 1;

    this.segments = new Map[ssize];

    for (int i = 0; i < numberOfSegments; i++) {

      this.segments[i] = Collections.synchronizedMap(new Segment((int) Math.ceil(capacity / numberOfSegments)));
    }

  }

  /**
   * Applies a supplemental hash function to a given hashCode, which defends against poor quality hash functions.
   */
  private static int hash(int h) {
    h += (h << 15) ^ 0xffffcd7d;
    h ^= (h >>> 10);
    h += (h << 3);
    h ^= (h >>> 6);
    h += (h << 2) + (h << 14);
    return h ^ (h >>> 16);
  }

  private int getIndexFromKey(Object key) {
    int hash = hash(key.hashCode());
    return (hash >>> segmentShift) & segmentMask;
  }

  private Map<K, V> segmentFor(final K key) {
    int index = getIndexFromKey(key);
    return this.segments[index];
  }

  public void clear() {
    for (int i = 0; i < segments.length; i++) {
      this.segments[i].clear();
    }
  }

  public boolean containsKey(K key) {
    return segmentFor(key).containsKey(key);
  }

  public boolean containsValue(Object value) {

    for (int i = 0; i < segments.length; i++) {
      if (this.segments[i].containsValue(value)) { return true; }
    }

    return false;
  }

  public V get(K key) {
    return segmentFor(key).get(key);
  }

  public boolean isEmpty() {
    for (int i = 0; i < segments.length; i++) {
      if (!this.segments[i].isEmpty()) { return false; }
    }

    return true;
  }

  public V put(K key, V value) {
    return segmentFor(key).put(key, value);
  }

  public V remove(K key) {
    return segmentFor(key).remove(key);
  }

  public int size() {
    int size = 0;
    for (int i = 0; i < segments.length; i++) {
      size += this.segments[i].size();
    }

    return size;
  }

  private final class Segment extends LinkedHashMap<K, V> {

    private final int segmentCapacity;

    public Segment(int capacity) {
      super(capacity, 0.75f, true);
      this.segmentCapacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
      return size() > segmentCapacity;
    }

  }

}
