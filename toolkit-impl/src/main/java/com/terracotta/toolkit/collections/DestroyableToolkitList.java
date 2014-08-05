/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;
import com.terracotta.toolkit.util.ToolkitObjectStatus;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class DestroyableToolkitList<E> extends AbstractDestroyableToolkitObject<ToolkitListInternal> implements
    ToolkitListInternal<E>, RejoinAwareToolkitObject {

  private volatile ToolkitListInternal<E>                      list;
  private final String                                         name;
  private final IsolatedClusteredObjectLookup<ToolkitListImpl> lookup;

  public DestroyableToolkitList(ToolkitObjectFactory factory, IsolatedClusteredObjectLookup<ToolkitListImpl> lookup,
                                ToolkitListImpl<E> list, String name, PlatformService platformService) {
    super(factory, platformService);
    this.lookup = lookup;
    this.list = list;
    this.name = name;
    list.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void doRejoinStarted() {
    this.list = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitListInternal.class);
  }

  @Override
  public void doRejoinCompleted() {
    if (!isDestroyed()) {
      ToolkitListImpl afterRejoin = lookup.lookupClusteredObject(name, ToolkitObjectType.LIST, null);
      if (afterRejoin == null) {
        destroyApplicator.applyDestroy();
      } else {
        this.list = afterRejoin;
      }
    }
  }

  @Override
  public void applyDestroy() {
    // status.setDestroyed() is called from Parent class
    this.list = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitListInternal.class);
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
    return new StatusAwareIterator(list.iterator(), status);
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
    return new SubListWrapper(list.subList(fromIndex, toIndex), status);
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
    private final List<E>              subList;
    private final int                  currentRejoinCount;
    private final ToolkitObjectStatus subTypeStatus;

    public SubListWrapper(List<E> subList, ToolkitObjectStatus status) {
      this.subList = subList;
      this.currentRejoinCount = status.getCurrentRejoinCount();
      this.subTypeStatus = status;
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
      return new StatusAwareIterator(subList.iterator(), this.subTypeStatus);
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
      return new SubListWrapper(subList.subList(fromIndex, toIndex), status);
    }

    private void checkDestroyedOrRejoined() {
      if (subTypeStatus.isDestroyed()) { throw new IllegalStateException(
                                                                         "The List backing this subList is already destroyed."); }
      if (this.currentRejoinCount != subTypeStatus.getCurrentRejoinCount()) { throw new RejoinException(
                                                                                                        "Rejoin has Occured, This sublist is not usable after rejoin anymore"); }
    }
  }

  @Override
  public boolean unlockedAdd(E e) {
    return list.unlockedAdd(e);
  }

}
