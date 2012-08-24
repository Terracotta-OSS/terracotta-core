/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

public class DestroyableToolkitSortedSet<E extends Comparable<? super E>> extends
    AbstractDestroyableToolkitObject<ToolkitSortedSet> implements ToolkitSortedSet<E> {

  private final String                 name;
  private volatile ToolkitSortedSet<E> set;

  public DestroyableToolkitSortedSet(ToolkitObjectFactory<ToolkitSortedSet> factory, ToolkitSortedSetImpl<E> set,
                                     String name) {
    super(factory);
    this.set = set;
    this.name = name;
    set.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void applyDestroy() {
    this.set = DestroyedInstanceProxy.createNewInstance(ToolkitSortedSet.class, getName());
  }

  @Override
  public void doDestroy() {
    set.destroy();
  }

  public ToolkitReadWriteLock getReadWriteLock() {
    return set.getReadWriteLock();
  }

  public int size() {
    return set.size();
  }

  public boolean isEmpty() {
    return set.isEmpty();
  }

  public boolean contains(Object o) {
    return set.contains(o);
  }

  public Iterator<E> iterator() {
    return new DestroyableIterator(set.iterator(), this);
  }

  public Comparator<? super E> comparator() {
    return set.comparator();
  }

  public Object[] toArray() {
    return set.toArray();
  }

  public SortedSet<E> subSet(E fromElement, E toElement) {
    return new SubSetWrapper(set.subSet(fromElement, toElement));
  }

  public <T> T[] toArray(T[] a) {
    return set.toArray(a);
  }

  public SortedSet<E> headSet(E toElement) {
    return new SubSetWrapper(set.headSet(toElement));
  }

  public boolean add(E e) {
    return set.add(e);
  }

  public SortedSet<E> tailSet(E fromElement) {
    return new SubSetWrapper(set.tailSet(fromElement));
  }

  public boolean remove(Object o) {
    return set.remove(o);
  }

  public E first() {
    return set.first();
  }

  public E last() {
    return set.last();
  }

  public boolean containsAll(Collection<?> c) {
    return set.containsAll(c);
  }

  public boolean addAll(Collection<? extends E> c) {
    return set.addAll(c);
  }

  public boolean retainAll(Collection<?> c) {
    return set.retainAll(c);
  }

  public boolean removeAll(Collection<?> c) {
    return set.removeAll(c);
  }

  public void clear() {
    set.clear();
  }

  @Override
  public boolean equals(Object o) {
    return set.equals(o);
  }

  @Override
  public int hashCode() {
    return set.hashCode();
  }

  @Override
  public String getName() {
    return name;
  }

  private class SubSetWrapper implements SortedSet<E> {
    private final SortedSet<E> subset;

    public SubSetWrapper(SortedSet<E> subset) {
      this.subset = subset;
    }

    public int size() {
      checkDestroyed();
      return subset.size();
    }

    public boolean isEmpty() {
      checkDestroyed();
      return subset.isEmpty();
    }

    public boolean contains(Object o) {
      checkDestroyed();
      return subset.contains(o);
    }

    public Iterator<E> iterator() {
      checkDestroyed();
      return new DestroyableIterator(subset.iterator(), DestroyableToolkitSortedSet.this);
    }

    public Comparator<? super E> comparator() {
      checkDestroyed();
      return subset.comparator();
    }

    public Object[] toArray() {
      checkDestroyed();
      return subset.toArray();
    }

    public SortedSet<E> subSet(E fromElement, E toElement) {
      checkDestroyed();
      return new SubSetWrapper(subset.subSet(fromElement, toElement));
    }

    public <T> T[] toArray(T[] a) {
      checkDestroyed();
      return subset.toArray(a);
    }

    public SortedSet<E> headSet(E toElement) {
      checkDestroyed();
      return new SubSetWrapper(subset.headSet(toElement));
    }

    public boolean add(E e) {
      checkDestroyed();
      return subset.add(e);
    }

    public SortedSet<E> tailSet(E fromElement) {
      checkDestroyed();
      return new SubSetWrapper(subset.tailSet(fromElement));
    }

    public boolean remove(Object o) {
      checkDestroyed();
      return subset.remove(o);
    }

    public E first() {
      checkDestroyed();
      return subset.first();
    }

    public E last() {
      checkDestroyed();
      return subset.last();
    }

    public boolean containsAll(Collection<?> c) {
      checkDestroyed();
      return subset.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
      checkDestroyed();
      return subset.addAll(c);
    }

    public boolean retainAll(Collection<?> c) {
      checkDestroyed();
      return subset.retainAll(c);
    }

    public boolean removeAll(Collection<?> c) {
      checkDestroyed();
      return subset.removeAll(c);
    }

    public void clear() {
      checkDestroyed();
      subset.clear();
    }

    @Override
    public boolean equals(Object o) {
      checkDestroyed();
      return subset.equals(o);
    }

    @Override
    public int hashCode() {
      return subset.hashCode();
    }

    private void checkDestroyed() {
      if (isDestroyed()) { throw new IllegalStateException("The SortedSet backing this subset is already destroyed."); }
    }
  }

}
