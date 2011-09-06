/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.object.bytecode.ManagerUtil;

import java.util.Iterator;
import java.util.Set;

/**
 * Since a Set is just a wrapper around a HashMap we need to use this to find out about removes from iterators on sets.
 * This is how we are doing it.
 */
public class SetIteratorWrapper implements Iterator {
  private final Set      set;
  private final Iterator iterator;
  private Object         current;

  public SetIteratorWrapper(Iterator iterator, Set set) {
    this.set = set;
    this.iterator = iterator;
  }

  public boolean hasNext() {
    return this.iterator.hasNext();
  }

  public Object next() {
    current = iterator.next();
    return current;
  }

  public void remove() {
    ManagerUtil.checkWriteAccess(set);
    iterator.remove();
    ManagerUtil.logicalInvoke(set, "remove(Ljava/lang/Object;)Z", new Object[] { current });
  }
}
