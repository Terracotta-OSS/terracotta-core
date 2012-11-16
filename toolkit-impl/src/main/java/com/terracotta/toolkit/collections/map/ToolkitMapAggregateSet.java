/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import com.terracotta.toolkit.util.collections.AggregateMapIterator;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public abstract class ToolkitMapAggregateSet<E> extends AbstractSet<E> {

  protected final AggregateServerMap map;

  public ToolkitMapAggregateSet(AggregateServerMap map) {
    this.map = map;
  }

  public static class ClusteredMapAggregateKeySet<K, V> extends ToolkitMapAggregateSet<K> {

    public ClusteredMapAggregateKeySet(AggregateServerMap<K, V> map) {
      super(map);
    }

    @Override
    public boolean contains(final Object o) {
      return map.containsKey(o);
    }

    @Override
    public boolean remove(final Object o) {
      return map.remove(o) != null;
    }

    @Override
    public Iterator<K> iterator() {
      return new AggregateMapIterator<K>(map.iterator()) {

        @Override
        public Iterator<K> getClusterMapIterator(Map aMap) {
          return aMap.keySet().iterator();
        }
      };
    }

    @Override
    public int size() {
      return map.size();
    }

  }

  public static class ClusteredMapAggregateEntrySet<K, V> extends ToolkitMapAggregateSet<Map.Entry<K, V>> {

    public ClusteredMapAggregateEntrySet(AggregateServerMap<K, V> map) {
      super(map);
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {

      return new AggregateMapIterator<Map.Entry<K, V>>(map.iterator()) {

        @Override
        public Iterator<Map.Entry<K, V>> getClusterMapIterator(Map aMap) {
          return aMap.entrySet().iterator();
        }
      };
    }

    @Override
    public boolean contains(final Object o) {
      if (!(o instanceof Map.Entry)) { return false; }
      final Map.Entry<K, V> e = (Map.Entry<K, V>) o;
      final Object value = map.get(e.getKey());
      return value != null && value.equals(e.getValue());
    }

    @Override
    public boolean remove(final Object o) {
      if (!(o instanceof Map.Entry)) { return false; }
      final Map.Entry<K, V> e = (Map.Entry<K, V>) o;
      final Object value = map.get(e.getKey());
      if (value != null && value.equals(e.getValue())) {
        return map.remove(e.getKey()) != null;
      } else {
        return false;
      }
    }

    @Override
    public int size() {
      return map.size();
    }

  }

  public static class ClusteredMapAggregatedValuesCollection<K, V> extends AbstractCollection<V> {

    private final AggregateServerMap map;

    public ClusteredMapAggregatedValuesCollection(AggregateServerMap<K, V> map) {
      this.map = map;
    }

    @Override
    public Iterator<V> iterator() {

      return new AggregateMapIterator<V>(map.iterator()) {

        @Override
        public Iterator<V> getClusterMapIterator(Map aMap) {
          return aMap.values().iterator();
        }

      };
    }

    @Override
    public int size() {
      return map.size();
    }
  }
}
