/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package java.util.concurrent;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCMapEntry;
import com.tcclient.util.ConcurrentHashMapEntrySetWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
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

  /*
   * ConcurrentHashMap uses the hashcode of the key and identify the segment to use. Each segment is an ReentrantLock.
   * This prevents multiple threads to update the same segment at the same time. To support in DSO, we need to check if
   * the ConcurrentHashMap is a shared object. If it is, we check if the hashcode of the key is the same as the
   * System.identityHashCode. If it is, we will use the DSO ObjectID of the key to be the hashcode. Since the ObjectID
   * of the key is a cluster-wide constant, different node will identify the same segment based on the ObjectID of the
   * key. If the hashcode of the key is not the same as the System.identityHashCode, that would mean the application has
   * defined the hashcode of the key and in this case, we could use honor the application defined hashcode of the key.
   * The reason that we do not want to always use the ObjectID of the key is because if the application has defined the
   * hashcode of the key, map.get(key1) and map.get(key2) will return the same object if key1 and key2 has the same
   * application defined hashcode even though key1 and key2 has 2 different ObjectID. Using ObjectID as the hashcode in
   * this case will prevent map.get(key1) and map.get(key2) to return the same result. If the application has not
   * defined the hashcode of the key, key1 and key2 will have 2 different hashcode (due to the fact that they will have
   * different System.identityHashCode). Therefore, map.get(key1) and map.get(key2) will return different objects. In
   * this case, using ObjectID will have the proper behavior. One limitation is that if the application define the
   * hashcode as some combination of system specific data such as a combination of System.identityHashCode() and some
   * other data, the current support of ConcurrentHashMap does not support this scenario. Another limitation is that if
   * the application defined hashcode of the key happens to be the same as the System.identityHashCode, the current
   * support of ConcurrentHashMap does not support this scenario either.
   */
  private int __tc_hash(Object obj) {
    return __tc_hash(obj, true);
  }

  private int __tc_hash(Object obj, boolean flag) {
    int i = obj.hashCode();
    boolean useObjectIDHashCode = false;

    if (System.identityHashCode(obj) == i) {
      if (flag) {
        if (__tc_managed() != null || ManagerUtil.isCreationInProgress())
          useObjectIDHashCode = true;
      } else {
        useObjectIDHashCode = true;
      }
    }

    if (useObjectIDHashCode) {
      TCObject tcobject = ManagerUtil.shareObjectIfNecessary(obj);
      if (tcobject != null) {
        i = tcobject.getObjectID().hashCode();
      }
    }

    i += ~(i << 9);
    i ^= i >>> 14;
    i += i << 4;
    i ^= i >>> 10;

    return i;
  }

  private boolean __tc_isDsoHashRequired(Object obj) {
    return __tc_managed() == null
      || ManagerUtil.lookupExistingOrNull(obj) != null
      || obj.hashCode() != System.identityHashCode(obj);
  }


  /*
   * Provides access to the real entryset that is wrapped by the
   * ConcurrentHashMapEntrySetWrapper.
   */
  public Set __tc_delegateEntrySet() {
    Set set = entrySet();
    if (set instanceof ConcurrentHashMapEntrySetWrapper) {
      return ((ConcurrentHashMapEntrySetWrapper)set).getDelegateEntrySet();
    } else {
      return set;
    }
  }


  /*
   * CHM-wide locking methods
   */
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


  /*
   * TCMap methods
   */
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
      Map.Entry e = (Map.Entry)i.next();
      if (((TCMapEntry)e).__tc_isValueFaultedIn()) {
        index++;
        tmp[index] = new ConcurrentHashMapEntrySetWrapper.EntryWrapper(this, e);
      }
    }

    if (index < 0) { return Collections.EMPTY_LIST; }
    Object[] rv = new Object[index + 1];
    System.arraycopy(tmp, 0, rv, 0, index + 1);
    return Arrays.asList(rv);
  }

  /*
   * Clearable methods
   */
  public int __tc_clearReferences(int toClear) {
    if (!__tc_isManaged()) { throw new AssertionError("clearReferences() called on Unmanaged ConcurrentHashMap"); }
    try {
      __tc_fullyWriteLock();

      int cleared = 0;
      for (Iterator i = __tc_delegateEntrySet().iterator(); i.hasNext() && toClear > cleared;) {
        Map.Entry e = (Map.Entry)i.next();
        if (((TCMapEntry)e).__tc_isValueFaultedIn()
            && e.getValue() instanceof Manageable) {
          Manageable m = (Manageable)e.getValue();
          TCObject tcObject = m.__tc_managed();
          if (tcObject != null
              && !tcObject.recentlyAccessed()) {
            ((TCMapEntry)e).__tc_rawSetValue(tcObject.getObjectID());
            cleared++;
          }
        }
      }
      return cleared;
    } finally {
      __tc_fullyWriteUnlock();
    }
  }
  
  // TODO: ImplementMe XXX
  public void __tc_put_logical(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean isEvictionEnabled() {
    return evictionEnabled;
  }

  public void setEvictionEnabled(boolean enabled) {
    evictionEnabled = enabled;
  }
}