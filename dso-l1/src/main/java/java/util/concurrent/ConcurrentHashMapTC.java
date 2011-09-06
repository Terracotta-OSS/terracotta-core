/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util.concurrent;

import com.tc.object.TCObjectExternal;
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

/**
 * Note that not all the methods of TCMap are implemented here.<br>
 * <br>
 * The ones that can't be found in this class are implemented through byte-code instrumentation, by adapting or renaming
 * the original put and remove methods. This is done in the JavaUtilConcurrentHashMapAdapter class.
 */

@SuppressWarnings("unchecked")
public abstract class ConcurrentHashMapTC extends ConcurrentHashMap implements TCMap, Clearable, Manageable {

  private boolean evictionEnabled = true;

  // These abstract methods are merely here so that they can be referenced by
  // the code in this class. When the ConcurrentHashMapTC is merged into
  // ConcurrentHashMap class, these abstract methods are simply ignored. They
  // are implemented during the actual instrumentation of ConcurrentHashMap.
  abstract void __tc_fullyReadLock();

  abstract void __tc_fullyReadUnlock();

  /*
   * ConcurrentHashMap uses the hashcode() of the key to select the segment to use. For keys types that do NOT override
   * hashCode() (thus they inherit identity hashcode functionality form java.lang.Object), we use the DSO ObjectID of
   * the key to be the hashcode. Since the ObjectID of the key is a cluster-wide constant, different node will identify
   * the same segment based on the ObjectID of the key. The reason that we do not want to always use the ObjectID of the
   * key is because if the application has defined the hashcode of the key, map.get(key1) and map.get(key2) will return
   * the same object if key1 and key2 has the same application defined hashcode even though key1 and key2 has 2
   * different ObjectID. Using ObjectID as the hashcode in this case will prevent map.get(key1) and map.get(key2) to
   * return the same result. If the key type does not override hashCode(), key1 and key2 will have 2 different hashcode
   * (due to the fact that they will have different System.identityHashCode). Therefore, map.get(key1) and map.get(key2)
   * will return different objects. In this case, using ObjectID will have the proper behavior. One limitation is that
   * if the application implements hashCode() using just the identity hashcode or some other non-stable data, things
   * won't work correctly
   */
  protected int __tc_hash(Object obj) {
    int i;

    if (__tc_isManaged()) {
      try {
        i = ManagerUtil.calculateDsoHashCode(obj);
      } catch (IllegalArgumentException iae) {
        //
        throw new IllegalArgumentException(
                                           "An object of type ["
                                               + obj.getClass()
                                               + "] was added to a clustered ConcurrentHashMap but the object does not override hashCode() and was not previously added to clustered state before being added to the map. Please implement hashCode() and equals() on this type and/or share this object by referring to it from clustered state before adding it to this data structure.");
      }
    } else {
      i = obj.hashCode();
    }

    i += ~(i << 9);
    i ^= i >>> 14;
    i += i << 4;
    i ^= i >>> 10;

    return i;
  }

  /**
   * Check whether the given object might possibly be a CHM key. If a CHM is shared, then we will only use keys that
   * have stable hash codes across all nodes. Such a key must either itself be shared, or it must be a literal, or it
   * must override hashCode() (which doesn't really guarantee anything, but we can hope...). If an object could not
   * possibly be a CHM key then we can short- circuit lookups.
   * 
   * @return true if the object could plausibly be a lookup key.
   */
  boolean __tc_isPossibleKey(Object obj) {
    if (!__tc_isManaged()) { return true; }
    if (ManagerUtil.isLiteralInstance(obj)) { return true; }
    if (ManagerUtil.lookupExistingOrNull(obj) != null) { return true; }
    if (ManagerUtil.overridesHashCode(obj)) { return true; }
    return false;
  }

  /*
   * Provides access to the real entryset that is wrapped by the ConcurrentHashMapEntrySetWrapper.
   */
  public Set __tc_delegateEntrySet() {
    Set set = entrySet();
    if (set instanceof ConcurrentHashMapEntrySetWrapper) {
      return ((ConcurrentHashMapEntrySetWrapper) set).getDelegateEntrySet();
    } else {
      return set;
    }
  }

  /*
   * CHM-wide locking methods
   */
  private void __tc_fullyWriteLock() {
    for (Segment segment : segments) {
      segment.lock();
    }
  }

  private void __tc_fullyWriteUnlock() {
    for (Segment segment : segments) {
      segment.unlock();
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
      Map.Entry e = (Map.Entry) i.next();
      if (((TCMapEntry) e).__tc_isValueFaultedIn()) {
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
        Map.Entry e = (Map.Entry) i.next();
        if (((TCMapEntry) e).__tc_isValueFaultedIn()) {
          TCObjectExternal tcObject = ManagerUtil.lookupExistingOrNull(e.getValue());
          if (tcObject != null && !tcObject.recentlyAccessed()) {
            ((TCMapEntry) e).__tc_rawSetValue(tcObject.getObjectID());
            cleared++;
          }
        }
      }
      return cleared;
    } finally {
      __tc_fullyWriteUnlock();
    }
  }

  @Override
  public void putAll(Map t) {
    if (__tc_isManaged()) {
      if (t.isEmpty()) return;

      for (Iterator i = t.entrySet().iterator(); i.hasNext();) {
        Entry e = (Entry) i.next();
        __tc_put_logical(e.getKey(), e.getValue());
      }
    } else {
      super.putAll(t);
    }
  }

  public boolean isEvictionEnabled() {
    return evictionEnabled;
  }

  public void setEvictionEnabled(boolean enabled) {
    evictionEnabled = enabled;
  }
}
