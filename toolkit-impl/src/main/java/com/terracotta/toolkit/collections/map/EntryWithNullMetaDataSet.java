/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata.EntryWithMetaData;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Abhishek Sanoujam
 */
public class EntryWithNullMetaDataSet<K, V> implements Set<EntryWithMetaData<K, V>> {

  private final Set<Entry<K, V>> entrySet;

  public EntryWithNullMetaDataSet(Set<Entry<K, V>> entrySet) {
    this.entrySet = entrySet;
    //
  }

  public Iterator<EntryWithMetaData<K, V>> iterator() {
    return new EntryWithNullMetaDataSetIterator(entrySet.iterator());
  }

  public int size() {
    return entrySet.size();
  }

  public boolean isEmpty() {
    return entrySet.isEmpty();
  }

  public boolean contains(Object o) {
    return entrySet.contains(o);
  }

  public Object[] toArray() {
    return entrySet.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return entrySet.toArray(a);
  }

  public boolean add(EntryWithMetaData<K, V> e) {
    return entrySet.add(e);
  }

  public boolean remove(Object o) {
    return entrySet.remove(o);
  }

  public boolean containsAll(Collection<?> c) {
    return entrySet.containsAll(c);
  }

  public boolean addAll(Collection<? extends EntryWithMetaData<K, V>> c) {
    return entrySet.addAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return entrySet.retainAll(c);
  }

  public boolean removeAll(Collection<?> c) {
    return entrySet.removeAll(c);
  }

  public void clear() {
    entrySet.clear();
  }

  @Override
  public boolean equals(Object o) {
    return entrySet.equals(o);
  }

  @Override
  public int hashCode() {
    return entrySet.hashCode();
  }

  private static class EntryWithNullMetaDataSetIterator<K, V> implements Iterator<EntryWithMetaData<K, V>> {
    private final Iterator<Entry<K, V>> iterator;

    public EntryWithNullMetaDataSetIterator(Iterator<Entry<K, V>> iterator) {
      this.iterator = iterator;
    }

    public EntryWithMetaData<K, V> next() {
      Entry<K, V> next = iterator.next();
      return new EntryWithMetaDataImpl(next.getKey(), next.getValue(), null);
    }

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public void remove() {
      iterator.remove();
    }
  }

}
