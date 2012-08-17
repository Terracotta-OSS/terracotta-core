/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections;

import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;

import java.util.Collection;
import java.util.Iterator;

public class DestroyableToolkitSet<E> extends AbstractDestroyableToolkitObject<ToolkitSet> implements ToolkitSet<E> {

  private final String           name;
  private volatile ToolkitSet<E> set;

  public DestroyableToolkitSet(ToolkitObjectFactory<ToolkitSet> factory, ToolkitSetImpl<E> set, String name) {
    super(factory);
    this.set = set;
    this.name = name;
    set.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void afterDestroy() {
    this.set = DestroyedInstanceProxy.createNewInstance(ToolkitSet.class, getName());
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

  public Object[] toArray() {
    return set.toArray();
  }

  public <T> T[] toArray(T[] a) {
    return set.toArray(a);
  }

  public boolean add(E e) {
    return set.add(e);
  }

  public boolean remove(Object o) {
    return set.remove(o);
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

}
