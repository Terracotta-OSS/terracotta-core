/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSortedSet;

import com.tc.object.ObjectID;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;

public class ToolkitSortedSetImpl<E extends Comparable<? super E>> extends ToolkitSetImpl<E> implements
    ToolkitSortedSet<E> {
  private final TreeMap<E, ObjectID> treeMap;

  public ToolkitSortedSetImpl() {
    super(new TreeMap());
    this.treeMap = (TreeMap<E, ObjectID>) localMap;
  }

  @Override
  public E first() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return treeMap.firstKey();
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public E last() {
    readLock();
    try {
      synchronized (localResolveLock) {
        applyPendingChanges();
        return treeMap.lastKey();
      }
    } finally {
      readUnlock();
    }
  }

  @Override
  public Comparator<? super E> comparator() {
    return null;
  }

  @Override
  public SortedSet<E> subSet(E fromElement, E toElement) {
    readLock();
    try {
      return new SubTerracottaSortedSet(fromElement, toElement);
    } finally {
      readUnlock();
    }
  }

  @Override
  public SortedSet<E> headSet(E toElement) {
    readLock();
    try {
      return new SubTerracottaSortedSet(null, toElement);
    } finally {
      readUnlock();
    }
  }

  @Override
  public SortedSet<E> tailSet(E fromElement) {
    readLock();
    try {
      return new SubTerracottaSortedSet(fromElement, null);
    } finally {
      readUnlock();
    }
  }

  private class SubTerracottaSortedSet implements SortedSet<E> {
    private final E            fromElement;
    private final E            toElement;
    private final SortedSet<E> localSubSet;
    private final boolean      toInclusive;

    public SubTerracottaSortedSet(E fromElement, E toElement) {
      this.fromElement = fromElement;
      this.toElement = toElement;
      if (fromElement == null && toElement == null) {
        throw new IllegalArgumentException();
      } else if (fromElement == null) {
        toInclusive = false;
        localSubSet = ((SortedSet) treeMap.keySet()).headSet(toElement);
      } else if (toElement == null) {
        toInclusive = true;
        localSubSet = ((SortedSet) treeMap.keySet()).tailSet(fromElement);
      } else {
        toInclusive = false;
        if (compare(fromElement, toElement) > 0) { throw new IllegalArgumentException("fromElement > toElement"); }
        localSubSet = ((SortedSet) treeMap.keySet()).subSet(fromElement, toElement);
      }
    }

    @Override
    public E first() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.first();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public E last() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.last();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public int size() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.size();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean isEmpty() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.isEmpty();
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean contains(Object o) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.contains(o);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public Iterator<E> iterator() {
      applyPendingChanges();
      return new SimpleSetIterator(localSubSet.iterator());
    }

    @Override
    public Object[] toArray() {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.toArray();
        }

      } finally {
        readUnlock();
      }
    }

    @Override
    public <T> T[] toArray(T[] a) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.toArray(a);
        }
      } finally {
        readUnlock();
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      readLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          return localSubSet.containsAll(c);
        }

      } finally {
        readUnlock();
      }

    }

    /**
     * Compares two keys using the correct comparison method for this TreeMap.
     */
    private final int compare(Object o1, Object o2) {
      return ((Comparable<? super E>) o1).compareTo((E) o2);
    }

    private final boolean inRange(Object key) {
      return !tooLow(key) && !tooHigh(key);
    }

    private final boolean tooLow(Object key) {
      if (fromElement != null) {
        int c = compare(key, fromElement);
        if (c < 0) return true;
      }
      return false;
    }

    private final boolean tooHigh(Object key) {
      if (toElement != null) {
        int c = compare(key, toElement);
        if (c > 0 || (c == 0 && !toInclusive)) return true;
      }
      return false;
    }

    private void checkInRange(Object o) {
      if (!inRange(o)) { throw new IllegalArgumentException("Element Out of Range"); }
    }

    private void checkAllInRange(Collection<?> c) {
      for (Object o : c) {
        if (!inRange(o)) { throw new IllegalArgumentException("Element Out of Range"); }
      }
    }

    @Override
    public boolean add(E e) {
      checkInRange(e);
      return ToolkitSortedSetImpl.this.add(e);
    }

    @Override
    public boolean remove(Object o) {
      checkInRange(o);
      return ToolkitSortedSetImpl.this.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
      checkAllInRange(c);
      return ToolkitSortedSetImpl.this.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      checkAllInRange(c);
      return ToolkitSortedSetImpl.this.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      checkAllInRange(c);
      return ToolkitSortedSetImpl.this.removeAll(c);
    }

    @Override
    public void clear() {
      writeLock();
      try {
        synchronized (localResolveLock) {
          applyPendingChanges();
          for (Object o : localSubSet) {
            unlockedRemove(o);
          }
        }
      } finally {
        writeUnlock();
      }
    }

    @Override
    public Comparator<? super E> comparator() {
      return ToolkitSortedSetImpl.this.comparator();
    }

    @Override
    public SortedSet<E> subSet(E from, E to) {
      checkInRange(from);
      checkInRange(to);
      return ToolkitSortedSetImpl.this.subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<E> headSet(E to) {
      checkInRange(to);
      if (fromElement != null) {
        return ToolkitSortedSetImpl.this.subSet(fromElement, to);
      } else {
        return ToolkitSortedSetImpl.this.headSet(toElement);
      }
    }

    @Override
    public SortedSet<E> tailSet(E from) {
      checkInRange(from);
      if (toElement != null) {
        return ToolkitSortedSetImpl.this.subSet(from, toElement);
      } else {
        return ToolkitSortedSetImpl.this.tailSet(from);
      }
    }
  }

}
