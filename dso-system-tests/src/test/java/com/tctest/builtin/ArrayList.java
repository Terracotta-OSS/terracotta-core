/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest.builtin;

import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.ManagerUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ArrayList<E> implements List<E> {

  private final java.util.List<E> data;

  public ArrayList() {
    data = new java.util.ArrayList<E>();
  }

  public ArrayList(int size) {
    data = new java.util.ArrayList<E>(size);
  }

  public ArrayList(List<E> l) {
    data = new java.util.ArrayList(l);
  }

  public int size() {
    return data.size();
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public boolean contains(Object o) {
    return data.contains(o);
  }

  public Iterator<E> iterator() {
    final Iterator<E> iter = data.iterator();
    return new Iterator<E>() {

      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public E next() {
        return iter.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    };
  }

  public Object[] toArray() {
    return data.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return data.toArray(a);
  }

  public boolean add(E e) {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.ADD_SIGNATURE, new Object[] { e });
    return data.add(e);
  }

  public boolean remove(Object o) {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_SIGNATURE, new Object[] { o });
    return data.remove(o);
  }

  public void clear() {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.CLEAR_SIGNATURE, new Object[] {});
    data.clear();
  }

  public E remove(int index) {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_AT_SIGNATURE, new Object[] { index });
    return data.remove(index);
  }

  public E set(int index, E element) {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.SET_SIGNATURE, new Object[] { index, element });
    return data.set(index, element);
  }

  public boolean containsAll(Collection<?> c) {
    return data.containsAll(c);
  }

  public boolean addAll(Collection<? extends E> c) {
    boolean rv = false;
    for (E o : c) {
      if (add(o)) {
        rv = true;
      }
    }
    return rv;
  }

  public boolean addAll(int index, Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object o) {
    return data.equals(o);
  }

  @Override
  public int hashCode() {
    return data.hashCode();
  }

  public E get(int index) {
    return data.get(index);
  }

  public void add(int index, E element) {
    ManagerUtil.checkWriteAccess(this);
    ManagerUtil.logicalInvoke(this, SerializationUtil.ADD_AT_SIGNATURE, new Object[] { index, element });
    data.add(index, element);
  }

  public int indexOf(Object o) {
    return data.indexOf(o);
  }

  public int lastIndexOf(Object o) {
    return data.lastIndexOf(o);
  }

  public ListIterator<E> listIterator() {
    return listIterator(0);
  }

  public ListIterator<E> listIterator(int index) {
    final ListIterator<E> iter = data.listIterator(0);

    return new ListIterator<E>() {

      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public E next() {
        return iter.next();
      }

      @Override
      public boolean hasPrevious() {
        throw new UnsupportedOperationException();
      }

      @Override
      public E previous() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int nextIndex() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int previousIndex() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

      @Override
      public void set(E e) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void add(E e) {
        throw new UnsupportedOperationException();
      }
    };
  }

  public List<E> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

}
