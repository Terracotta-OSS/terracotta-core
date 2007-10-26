/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package java.util.concurrent;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCMapEntry;
import com.tcclient.util.ConcurrentHashMapEntrySetWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("unchecked")
/**
 * Note that not all the methods of TCMap are implemented here.
 * 
 * The ones that can't be found in this class are implemented through
 * byte-code instrumentation, by adapting or renaming the original put and
 * remove methods. This is done in the JavaUtilConcurrentHashMapAdapter class.
 */
public abstract class ConcurrentHashMapTC extends ConcurrentHashMap implements TCMap, Clearable, Manageable {
  private boolean evictionEnabled = true;

  // These abstract methods are merely here so that they can be referenced by
  // the code in this class. When the ConcurrentHashMapTC is merged into
  // ConcurrentHashMap class, these abstract methods are simply ignored. They
  // are implemented during the actual instrumentation of ConcurrentHashMap.
  abstract void __tc_fullyReadLock();
  abstract void __tc_fullyReadUnlock();
  
  public Set __tc_delegateEntrySet() {
    Set set = entrySet();
    if (set instanceof ConcurrentHashMapEntrySetWrapper) {
      return ((ConcurrentHashMapEntrySetWrapper)set).getDelegateEntrySet();
    } else {
      return set;
    }
  }

  private void __tc_fullyWriteLock() {
    for(int i = 0; i < segments.length; i++) {
      segments[i].lock();
    }
  }

  private void __tc_fullyWriteUnlock() {
    for(int i = 0; i < segments.length; i++) {
      segments[i].unlock();
    }
  }

  public Collection __tc_getAllEntriesSnapshot() {
    if (__tc_isManaged()) {
      try {
        __tc_fullyReadLock();
        return __tc_getAllEntriesSnapshotInternal();
      } finally {
        __tc_fullyReadUnlock();
      }
    } else {
      return __tc_getAllEntriesSnapshotInternal();
    }
  }

  public synchronized Collection __tc_getAllEntriesSnapshotInternal() {
    return new ArrayList(entrySet());
  }
  
  public Collection __tc_getAllLocalEntriesSnapshot() {
    if (__tc_isManaged()) {
      try {
        __tc_fullyReadLock();
        return __tc_getAllLocalEntriesSnapshotInternal();
      } finally {
        __tc_fullyReadUnlock();
      }
    } else {
      return __tc_getAllLocalEntriesSnapshotInternal();
    }
  }

  private Collection __tc_getAllLocalEntriesSnapshotInternal() {
    Set fullEntrySet = __tc_delegateEntrySet();
    int entrySetSize = fullEntrySet.size();
    if (entrySetSize == 0) { return Collections.EMPTY_LIST; }

    Object[] tmp = new Object[entrySetSize];
    int index = -1;
    for (Iterator i = fullEntrySet.iterator(); i.hasNext();) {
      TCMapEntry e = (TCMapEntry)i.next();
      if (e.__tc_isValueFaultedIn()) {
        index++;
        tmp[index] = new ConcurrentHashMapEntrySetWrapper.EntryWrapper(this, e);
      }
    }

    if (index < 0) { return Collections.EMPTY_LIST; }
    Object[] rv = new Object[index + 1];
    System.arraycopy(tmp, 0, rv, 0, index + 1);
    return Arrays.asList(rv);
  }
  
  public int __tc_clearReferences(int toClear) {
    if (!__tc_isManaged()) { throw new AssertionError("clearReferences() called on Unmanaged ConcurrentHashMap"); }
    try {
      __tc_fullyWriteLock();

      int cleared = 0;
      for (Iterator i = __tc_delegateEntrySet().iterator(); i.hasNext() && toClear > cleared;) {
        TCMapEntry e = (TCMapEntry) i.next();
        if (e.__tc_isValueFaultedIn()
            && e.getValue() instanceof Manageable) {
          Manageable m = (Manageable)e.getValue();
          TCObject tcObject = m.__tc_managed();
          if (tcObject != null
              && !tcObject.recentlyAccessed()) {
            e.__tc_rawSetValue(tcObject.getObjectID());
            cleared++;
          }
        }
      }
      return cleared;
    } finally {
      __tc_fullyWriteUnlock();
    }
  }

  public boolean isEvictionEnabled() {
    return evictionEnabled;
  }

  public void setEvictionEnabled(boolean enabled) {
    evictionEnabled = enabled;
  }
}