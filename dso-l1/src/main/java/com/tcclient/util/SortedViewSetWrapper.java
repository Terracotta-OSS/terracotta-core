/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.SetIteratorWrapper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * A wrapper for TreeSet.subSet(), TreeSet.headSet(), and TreeSet.tailSet() that keeps DSO informed of changes.
 */
public final class SortedViewSetWrapper implements SortedSet {
  public final static String  CLASS_SLASH = "com/tcclient/util/SortedViewSetWrapper";

  // viewSet is transient and is re-constructed from oringalSet,
  private transient SortedSet viewSet;
  // fromKey, toKey, and head.
  private SortedSet           originalSet;

  private Object              fromKey;
  private Object              toKey;
  private boolean             head;

  public SortedViewSetWrapper(SortedSet originalSet, SortedSet viewSet, Object fromKey, Object toKey) {
    this.originalSet = originalSet;
    this.viewSet = viewSet;
    this.fromKey = fromKey;
    this.toKey = toKey;
  }

  public SortedViewSetWrapper(SortedSet originalSet, SortedSet viewSet, Object key, boolean head) {
    this.originalSet = originalSet;
    this.viewSet = viewSet;
    this.head = head;
    if (head) {
      this.toKey = key;
    } else {
      this.fromKey = key;
    }
  }

  private SortedSet getViewSet() {
    if (viewSet == null) {
      if (head) {
        viewSet = ((SortedViewSetWrapper) originalSet.headSet(toKey)).viewSet;
      } else if (toKey == null) {
        viewSet = ((SortedViewSetWrapper) originalSet.tailSet(fromKey)).viewSet;
      } else {
        viewSet = ((SortedViewSetWrapper) originalSet.subSet(fromKey, toKey)).viewSet;
      }
    }
    return viewSet;
  }

  public final boolean add(Object o) {
    ManagerUtil.checkWriteAccess(originalSet);
    boolean flag = getViewSet().add(o);
    if (flag) {
      ManagerUtil.logicalInvoke(originalSet, SerializationUtil.ADD_SIGNATURE, new Object[] { o });
    }

    return flag;
  }

  public final boolean addAll(Collection c) {
    ManagerUtil.checkWriteAccess(originalSet);
    boolean flag = getViewSet().addAll(c);
    if (flag) {
      ManagerUtil.logicalInvoke(originalSet, SerializationUtil.ADD_ALL_SIGNATURE, new Object[] { c });
    }

    return flag;
  }

  public final void clear() {
    ManagerUtil.checkWriteAccess(originalSet);
    Object[] clearSet = getViewSet().toArray();
    getViewSet().clear();
    ManagerUtil.logicalInvoke(originalSet, SerializationUtil.REMOVE_ALL_SIGNATURE, clearSet);
  }

  public final boolean contains(Object o) {
    return getViewSet().contains(o);
  }

  public final boolean containsAll(Collection c) {
    return getViewSet().containsAll(c);
  }

  public final boolean equals(Object o) {
    return getViewSet().equals(o);
  }

  public final int hashCode() {
    return getViewSet().hashCode();
  }

  public final boolean isEmpty() {
    return getViewSet().isEmpty();
  }

  public final Iterator iterator() {
    return new SetIteratorWrapper(getViewSet().iterator(), originalSet);
  }

  public final boolean remove(Object o) {
    ManagerUtil.checkWriteAccess(originalSet);
    boolean removed = getViewSet().remove(o);
    if (removed) {
      ManagerUtil.logicalInvoke(originalSet, SerializationUtil.REMOVE_SIGNATURE, new Object[] { o });
    }
    return removed;
  }

  public final boolean removeAll(Collection c) {
    boolean modified = false;

    if (size() > c.size()) {
      for (Iterator i = c.iterator(); i.hasNext();)
        modified |= remove(i.next());
    } else {
      for (Iterator i = iterator(); i.hasNext();) {
        if (c.contains(i.next())) {
          i.remove();
          modified = true;
        }
      }
    }
    return modified;
  }

  public final boolean retainAll(Collection c) {
    boolean modified = false;
    Iterator i = iterator();
    while (i.hasNext()) {
      if (!c.contains(i.next())) {
        i.remove();
        modified = true;
      }
    }
    return modified;

  }

  public final int size() {
    return getViewSet().size();
  }

  public final Object[] toArray() {
    return getViewSet().toArray();
  }

  public final Object[] toArray(Object[] a) {
    int size = size();
    if (a.length < size) a = (Object[]) Array.newInstance(((Object) (a)).getClass().getComponentType(), size);

    int index = 0;
    for (Iterator iterator = iterator(); iterator.hasNext();) {
      ManagerUtil.objectArrayChanged(a, index++, iterator.next());
    }

    if (a.length > size) {
      a[size] = null;
    }
    return a;
  }

  public final Comparator comparator() {
    return getViewSet().comparator();
  }

  public final SortedSet subSet(Object fromElement, Object toElement) {
    return new SortedViewSetWrapper(originalSet,
                                    ((SortedViewSetWrapper) viewSet.subSet(fromElement, toElement)).viewSet,
                                    fromElement, toElement);
  }

  public final SortedSet headSet(Object toElement) {
    return new SortedViewSetWrapper(originalSet, ((SortedViewSetWrapper) viewSet.headSet(toElement)).viewSet,
                                    toElement, true);
  }

  public final SortedSet tailSet(Object fromElement) {
    return new SortedViewSetWrapper(originalSet, ((SortedViewSetWrapper) viewSet.tailSet(fromElement)).viewSet,
                                    fromElement, false);
  }

  public final Object first() {
    return getViewSet().first();
  }

  public final Object last() {
    return getViewSet().last();
  }

  public String toString() {
    return viewSet.toString();
  }
}
