/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.ManagerUtil;

import java.util.List;
import java.util.ListIterator;

public class ListIteratorWrapper implements ListIterator {

  private final List         list;
  private final ListIterator realIterator;
  private Object             current;
  private int                currentIndex;

  public ListIteratorWrapper(List list, ListIterator realIterator) {
    this.list = list;
    this.realIterator = realIterator;
    this.currentIndex = -1;
  }

  public void add(Object arg0) {
    ManagerUtil.checkWriteAccess(list);
    currentIndex = nextIndex();
    realIterator.add(arg0);
    current = arg0;
    ManagerUtil.logicalInvoke(list, SerializationUtil.ADD_AT_SIGNATURE, new Object[] { Integer.valueOf(currentIndex),
        arg0 });
  }

  public boolean hasNext() {
    return realIterator.hasNext();
  }

  public boolean hasPrevious() {
    return realIterator.hasPrevious();
  }

  public Object next() {
    currentIndex = nextIndex();
    current = realIterator.next();
    return current;
  }

  public int nextIndex() {
    return realIterator.nextIndex();
  }

  public Object previous() {
    currentIndex = previousIndex();
    current = realIterator.previous();
    return current;
  }

  public int previousIndex() {
    return realIterator.previousIndex();
  }

  public void remove() {
    ManagerUtil.checkWriteAccess(list);
    realIterator.remove();
    ManagerUtil.logicalInvoke(list, SerializationUtil.REMOVE_AT_SIGNATURE,
                              new Object[] { Integer.valueOf(currentIndex) });
  }

  public void set(Object arg0) {
    ManagerUtil.checkWriteAccess(list);
    realIterator.set(arg0);
    current = arg0;
    ManagerUtil.logicalInvoke(list, SerializationUtil.SET_SIGNATURE, new Object[] { Integer.valueOf(currentIndex),
        current });
  }

}
