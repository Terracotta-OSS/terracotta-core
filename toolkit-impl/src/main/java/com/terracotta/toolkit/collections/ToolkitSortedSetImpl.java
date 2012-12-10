/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.rejoin.RejoinException;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.locks.ReadWriteLock;

public class ToolkitSortedSetImpl<E extends Comparable<? super E>> extends ToolkitSetImpl<E> implements
    ToolkitSortedSet<E> {
  private final DestroyableToolkitSortedMap<E, Integer> sortedMap;
  private int                                           currentRejoinCount;

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
    return null;
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
    private final int                   rejoinCount;

    public SubTerracottaSortedSet(SortedMap<E, Integer> internalSortedMap) {
      this.internalSortedMap = internalSortedMap;
      this.rejoinCount = ToolkitSortedSetImpl.this.currentRejoinCount;
    }

    @Override
    public E first() {
      exceptionIfRejoinOccured();
      return internalSortedMap.firstKey();
    }

    private void exceptionIfRejoinOccured() {
      if (this.rejoinCount != ToolkitSortedSetImpl.this.currentRejoinCount) { throw new RejoinException(
                                                                                                        "SubSet is not usable once rejoin has occured"); }
    }

    @Override
    public E last() {
      exceptionIfRejoinOccured();
      return internalSortedMap.lastKey();
    }

    @Override
    public int size() {
      exceptionIfRejoinOccured();
      return internalSortedMap.size();
    }

    @Override
    public boolean isEmpty() {
      exceptionIfRejoinOccured();
      return internalSortedMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().containsAll(c);
    }

    @Override
    public boolean add(E e) {
      exceptionIfRejoinOccured();
      return internalSortedMap.put(e, DUMMY_VALUE) == null;
    }

    @Override
    public boolean remove(Object o) {
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
      exceptionIfRejoinOccured();
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
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      exceptionIfRejoinOccured();
      return internalSortedMap.keySet().removeAll(c);
    }

    @Override
    public void clear() {
      exceptionIfRejoinOccured();
      internalSortedMap.clear();
    }

    @Override
    public Comparator<? super E> comparator() {
      exceptionIfRejoinOccured();
      return ToolkitSortedSetImpl.this.comparator();
    }

    @Override
    public SortedSet<E> subSet(E from, E to) {
      exceptionIfRejoinOccured();
      return new SubTerracottaSortedSet(internalSortedMap.subMap(from, to));
    }

    @Override
    public SortedSet<E> headSet(E to) {
      exceptionIfRejoinOccured();
      return new SubTerracottaSortedSet(internalSortedMap.headMap(to));
    }

    @Override
    public SortedSet<E> tailSet(E from) {
      exceptionIfRejoinOccured();
      return new SubTerracottaSortedSet(internalSortedMap.tailMap(from));
    }
  }

}
