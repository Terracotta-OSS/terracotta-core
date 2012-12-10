/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class DestroyableToolkitList<E> extends AbstractDestroyableToolkitObject<ToolkitList> implements ToolkitList<E>,
    RejoinAwareToolkitObject {

  private volatile ToolkitList<E>                              list;
  private final String                                         name;
  private final IsolatedClusteredObjectLookup<ToolkitListImpl> lookup;
  private volatile int                                         rejoinCount;

  public DestroyableToolkitList(ToolkitObjectFactory factory, IsolatedClusteredObjectLookup<ToolkitListImpl> lookup,
                                ToolkitListImpl<E> list, String name) {
    super(factory);
    this.lookup = lookup;
    this.list = list;
    this.name = name;
    list.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void rejoinStarted() {
    this.list = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitList.class);
    rejoinCount++;
  }

  @Override
  public void rejoinCompleted() {
    ToolkitListImpl afterRejoin = lookup.lookupClusteredObject(name);
    if (afterRejoin != null) {
      this.list = afterRejoin;
    } else {
      // didn't find backing clustered object after rejoin - must have been destroyed
      // apply destroy locally
      applyDestroy();
    }
  }

  @Override
  public void applyDestroy() {
    this.list = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitList.class);
  }

  @Override
  public void doDestroy() {
    list.destroy();
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return list.contains(o);
  }

  @Override
  public Iterator<E> iterator() {
    return new DestroyableIterator(list.iterator(), this);
  }

  @Override
  public Object[] toArray() {
    return list.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return list.toArray(a);
  }

  @Override
  public boolean add(E e) {
    return list.add(e);
  }

  @Override
  public boolean remove(Object o) {
    return list.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return list.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    return list.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return list.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return list.retainAll(c);
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public E get(int index) {
    return list.get(index);
  }

  @Override
  public E set(int index, E element) {
    return list.set(index, element);
  }

  @Override
  public void add(int index, E element) {
    list.add(index, element);
  }

  @Override
  public E remove(int index) {
    return list.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return list.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }

  @Override
  public ListIterator<E> listIterator() {
    return list.listIterator();
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    return list.listIterator(index);
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    return new SubListWrapper(list.subList(fromIndex, toIndex));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public ToolkitReadWriteLock getReadWriteLock() {
    return list.getReadWriteLock();
  }

  private class SubListWrapper implements List<E> {
    private final List<E> subList;
    private final int     rejoinCount;

    public SubListWrapper(List<E> subList) {
      this.subList = subList;
      this.rejoinCount = DestroyableToolkitList.this.rejoinCount;
    }

    @Override
    public int size() {
      checkDestroyedOrRejoined();
      return subList.size();
    }

    @Override
    public boolean isEmpty() {
      checkDestroyedOrRejoined();
      return subList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      checkDestroyedOrRejoined();
      return subList.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
      checkDestroyedOrRejoined();
      return new DestroyableIterator(subList.iterator(), DestroyableToolkitList.this);
    }

    @Override
    public Object[] toArray() {
      checkDestroyedOrRejoined();
      return subList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      checkDestroyedOrRejoined();
      return subList.toArray(a);
    }

    @Override
    public boolean add(E e) {
      checkDestroyedOrRejoined();
      return subList.add(e);
    }

    @Override
    public boolean remove(Object o) {
      checkDestroyedOrRejoined();
      return subList.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      checkDestroyedOrRejoined();
      return subList.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
      checkDestroyedOrRejoined();
      return subList.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
      checkDestroyedOrRejoined();
      return subList.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      checkDestroyedOrRejoined();
      return subList.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      checkDestroyedOrRejoined();
      return subList.retainAll(c);
    }

    @Override
    public void clear() {
      checkDestroyedOrRejoined();
      subList.clear();
    }

    @Override
    public boolean equals(Object o) {
      checkDestroyedOrRejoined();
      return subList.equals(o);
    }

    @Override
    public int hashCode() {
      checkDestroyedOrRejoined();
      return subList.hashCode();
    }

    @Override
    public E get(int index) {
      checkDestroyedOrRejoined();
      return subList.get(index);
    }

    @Override
    public E set(int index, E element) {
      checkDestroyedOrRejoined();
      return subList.set(index, element);
    }

    @Override
    public void add(int index, E element) {
      checkDestroyedOrRejoined();
      subList.add(index, element);
    }

    @Override
    public E remove(int index) {
      checkDestroyedOrRejoined();
      return subList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
      checkDestroyedOrRejoined();
      return subList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
      checkDestroyedOrRejoined();
      return subList.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
      checkDestroyedOrRejoined();
      return subList.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
      checkDestroyedOrRejoined();
      return subList.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
      checkDestroyedOrRejoined();
      return new SubListWrapper(subList.subList(fromIndex, toIndex));
    }

    private void checkDestroyedOrRejoined() {
      if (isDestroyed()) { throw new IllegalStateException("The List backing this subList is already destroyed."); }
      if (this.rejoinCount != DestroyableToolkitList.this.rejoinCount) { throw new RejoinException(
                                                                                                   "Rejoin has Occured, This sublist is not usable after rejoin anymore"); }
    }

  }

}
