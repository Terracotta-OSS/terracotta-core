/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSortedSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.locks.ReadWriteLock;

public class ToolkitSortedSetImpl<E extends Comparable<? super E>> extends ToolkitSetImpl<E> implements
    ToolkitSortedSet<E> {
  private final DestroyableToolkitSortedMap<E, Integer> sortedMap;

  public ToolkitSortedSetImpl(DestroyableToolkitSortedMap<E, Integer> sortedMap) {
    super(sortedMap);
    this.sortedMap = sortedMap;

  }

  @Override
  public E first() {
    return sortedMap.firstKey();
  }

  @Override
  public E last() {
    return sortedMap.lastKey();
  }

  @Override
  public Comparator<? super E> comparator() {
    return sortedMap.comparator();
  }

  @Override
  public SortedSet<E> subSet(E fromElement, E toElement) {
    return new SubTerracottaSortedSet(sortedMap.subMap(fromElement, toElement));
  }

  @Override
  public SortedSet<E> headSet(E toElement) {
    return new SubTerracottaSortedSet(sortedMap.headMap(toElement));
  }

  @Override
  public SortedSet<E> tailSet(E fromElement) {
    return new SubTerracottaSortedSet(sortedMap.tailMap(fromElement));
  }

  private class SubTerracottaSortedSet implements SortedSet<E> {
    private final SortedMap<E, Integer> internalSortedMap;

    public SubTerracottaSortedSet(SortedMap<E, Integer> internalSortedMap) {
      this.internalSortedMap = internalSortedMap;
    }

    @Override
    public E first() {
      return internalSortedMap.firstKey();
    }

    @Override
    public E last() {
      return internalSortedMap.lastKey();
    }

    @Override
    public int size() {
      return internalSortedMap.size();
    }

    @Override
    public boolean isEmpty() {
      return internalSortedMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return internalSortedMap.keySet().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
      return internalSortedMap.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
      return internalSortedMap.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return internalSortedMap.keySet().toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return internalSortedMap.keySet().containsAll(c);
    }

    @Override
    public boolean add(E e) {
      return internalSortedMap.put(e, DUMMY_VALUE) == null;
    }

    @Override
    public boolean remove(Object o) {
      return internalSortedMap.keySet().remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {

      ReadWriteLock lock = getReadWriteLock();
      lock.writeLock().lock();

      try {
        int size = internalSortedMap.size();
        for (E e : c) {
          add(e);
        }

        return size < internalSortedMap.size();
      } finally {
        lock.writeLock().unlock();
      }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return internalSortedMap.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return internalSortedMap.keySet().removeAll(c);
    }

    @Override
    public void clear() {
      internalSortedMap.clear();
    }

    @Override
    public Comparator<? super E> comparator() {
      return internalSortedMap.comparator();
    }

    @Override
    public SortedSet<E> subSet(E from, E to) {
      return new SubTerracottaSortedSet(internalSortedMap.subMap(from, to));
    }

    @Override
    public SortedSet<E> headSet(E to) {
      return new SubTerracottaSortedSet(internalSortedMap.headMap(to));
    }

    @Override
    public SortedSet<E> tailSet(E from) {
      return new SubTerracottaSortedSet(internalSortedMap.tailMap(from));
    }
  }

}
