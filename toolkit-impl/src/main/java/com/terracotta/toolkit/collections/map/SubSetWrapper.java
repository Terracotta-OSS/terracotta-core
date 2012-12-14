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
  private final ToolkitObjectType    superTypeClassName;

  public SubSetWrapper(Set<E> set, ToolkitSubtypeStatus status, String name, ToolkitObjectType objectType) {
    super();
    this.status = status;
    this.set = set;
    this.rejoinCount = status.getCurrentRejoinCount();
    this.superTypeName = name;
    superTypeClassName = objectType;
  }

  @Override
  public int size() {
    checkDestryoedOrRejoined();
    return set.size();
  }

  @Override
  public boolean isEmpty() {
    checkDestryoedOrRejoined();
    return set.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    checkDestryoedOrRejoined();
    return set.contains(o);
  }

  @Override
  public Iterator iterator() {
    checkDestryoedOrRejoined();
    return set.iterator();
  }

  @Override
  public Object[] toArray() {
    checkDestryoedOrRejoined();
    return set.toArray();
  }

  @Override
  public Object[] toArray(Object[] a) {
    checkDestryoedOrRejoined();
    return set.toArray(a);
  }

  @Override
  public boolean add(E e) {
    checkDestryoedOrRejoined();
    return set.add(e);
  }

  @Override
  public boolean remove(Object o) {
    checkDestryoedOrRejoined();
    return set.remove(o);
  }

  @Override
  public boolean containsAll(Collection c) {
    checkDestryoedOrRejoined();
    return set.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    checkDestryoedOrRejoined();
    return set.addAll(c);
  }

  @Override
  public boolean removeAll(Collection c) {
    checkDestryoedOrRejoined();
    return set.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection c) {
    checkDestryoedOrRejoined();
    return set.retainAll(c);
  }

  @Override
  public void clear() {
    checkDestryoedOrRejoined();
    set.clear();
  }

  private void checkDestryoedOrRejoined() {
    if (status.isDestroyed()) { throw new IllegalStateException(
                                                                "The object "
                                                                    + this.superTypeName
                                                                    + " of type"
                                                                    + this.superTypeClassName
                                                                    + "  has already been destroyed, all SubTypes associated with are unusable "); }
    if (this.rejoinCount != status.getCurrentRejoinCount()) { throw new RejoinException(
                                                                                        "The SubTypes associated with "
                                                                                            + this.superTypeName
                                                                                            + " of type "
                                                                                            + this.superTypeClassName
                                                                                            + " are not usable anymore afer rejoin!"); }
  }

}
