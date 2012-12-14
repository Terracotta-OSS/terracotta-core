package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.rejoin.RejoinException;

import com.terracotta.toolkit.util.ToolkitSubtypeStatus;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

class SubSetWrapper<E> implements Set<E> {
  private final Set<E>               set;
  private final ToolkitSubtypeStatus status;
  private final int                  rejoinCount;
  private final String               superTypeName;
  private final ToolkitObjectType    toolkitObjectType;

  public SubSetWrapper(Set<E> set, ToolkitSubtypeStatus status, String name, ToolkitObjectType objectType) {
    super();
    this.status = status;
    this.set = set;
    this.rejoinCount = status.getCurrentRejoinCount();
    this.superTypeName = name;
    toolkitObjectType = objectType;
  }

  @Override
  public int size() {
    checkDestroyedOrRejoined();
    return set.size();
  }

  @Override
  public boolean isEmpty() {
    checkDestroyedOrRejoined();
    return set.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    checkDestroyedOrRejoined();
    return set.contains(o);
  }

  @Override
  public Iterator iterator() {
    checkDestroyedOrRejoined();
    return set.iterator();
  }

  @Override
  public Object[] toArray() {
    checkDestroyedOrRejoined();
    return set.toArray();
  }

  @Override
  public Object[] toArray(Object[] a) {
    checkDestroyedOrRejoined();
    return set.toArray(a);
  }

  @Override
  public boolean add(E e) {
    checkDestroyedOrRejoined();
    return set.add(e);
  }

  @Override
  public boolean remove(Object o) {
    checkDestroyedOrRejoined();
    return set.remove(o);
  }

  @Override
  public boolean containsAll(Collection c) {
    checkDestroyedOrRejoined();
    return set.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    checkDestroyedOrRejoined();
    return set.addAll(c);
  }

  @Override
  public boolean removeAll(Collection c) {
    checkDestroyedOrRejoined();
    return set.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection c) {
    checkDestroyedOrRejoined();
    return set.retainAll(c);
  }

  @Override
  public void clear() {
    checkDestroyedOrRejoined();
    set.clear();
  }

  private void checkDestroyedOrRejoined() {
    if (status.isDestroyed()) { throw new IllegalStateException(
                                                                "The object "
                                                                    + this.superTypeName
                                                                    + " of type "
                                                                    + this.toolkitObjectType
                                                                    + "  has already been destroyed, all SubTypes associated with are unusable "); }
    if (this.rejoinCount != status.getCurrentRejoinCount()) { throw new RejoinException(
                                                                                        "The SubTypes associated with "
                                                                                            + this.superTypeName
                                                                                            + " of type "
                                                                                            + this.toolkitObjectType
                                                                                            + " are not usable anymore afer rejoin!"); }
  }

}
