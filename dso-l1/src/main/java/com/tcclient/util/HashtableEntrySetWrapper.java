/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcclient.util;

import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * A wrapper for Map.entrySet() that keeps DSO informed of changes
 */
public class HashtableEntrySetWrapper extends MapEntrySetWrapper {
  public final static String CLASS = "com/tcclient/util/HashtableEntrySetWrapper";

  public HashtableEntrySetWrapper(Map map, Set realEntrySet) {
    super(map, realEntrySet);
  }

  public final Iterator iterator() {
    return new HashtableIteratorWrapper(map, realEntrySet.iterator());
  }

  public final boolean remove(Object o) {
    boolean removed = false;
    ManagerUtil.monitorEnter(map, Manager.LOCK_TYPE_WRITE);
    try {
      ManagerUtil.checkWriteAccess(map);
      removed = realEntrySet.remove(o);
      if (removed) {
        ManagerUtil.logicalInvoke(map, SerializationUtil.REMOVE_KEY_SIGNATURE,
                                  new Object[] { ((Map.Entry) o).getKey() });
      }
    } finally {
      ManagerUtil.monitorExit(map, Manager.LOCK_TYPE_WRITE);
    }
    return removed;
  }

  private static class HashtableIteratorWrapper implements Iterator {

    protected final Iterator realIterator;
    protected final Map      map;
    protected Entry          current;

    HashtableIteratorWrapper(Map map, Iterator realIterator) {
      this.map = map;
      this.realIterator = realIterator;
    }

    public final void remove() {
      ManagerUtil.monitorEnter(map, Manager.LOCK_TYPE_WRITE);
      try {
        realIterator.remove();

        // important to do this after the real remove() since an exception can be thrown (never
        // started, at end, concurrent mod, etc)
        ManagerUtil.logicalInvoke(map, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { current.getKey() });
      } finally {
        ManagerUtil.monitorExit(map, Manager.LOCK_TYPE_WRITE);
      }
    }

    public final boolean hasNext() {
      boolean rv = realIterator.hasNext();
      return rv;
    }

    public final Object next() {
      current = new EntryWrapper(map, (Entry) realIterator.next());
      return current;
    }

  }

}
