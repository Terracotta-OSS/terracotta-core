/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package java.util.concurrent;

import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("unchecked")
public abstract class ConcurrentHashMapTC extends ConcurrentHashMap implements TCMap, Manageable {
  public Collection __tc_getAllEntriesSnapshot() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return __tc_getAllEntriesSnapshotInternal();
      }
    } else {
      return __tc_getAllEntriesSnapshotInternal();
    }
  }

  public synchronized Collection __tc_getAllEntriesSnapshotInternal() {
    return new ArrayList(entrySet());
  }

  public Collection __tc_getAllLocalEntriesSnapshot() {
    throw new UnsupportedOperationException();
  }
  
  // This code doesn't work since the entry set doesn't contain the actual
  // entry values, but rather calls into the underlying get() method of the
  // ConcurrentHashMap.
//
//  public Collection __tc_getAllLocalEntriesSnapshot() {
//    if (__tc_isManaged()) {
//      synchronized (__tc_managed().getResolveLock()) {
//        return __tc_getAllLocalEntriesSnapshotInternal();
//      }
//    } else {
//      return __tc_getAllLocalEntriesSnapshotInternal();
//    }
//  }
//
//  private Collection __tc_getAllLocalEntriesSnapshotInternal() {
//    Set fullEntrySet = entrySet();
//    int entrySetSize = fullEntrySet.size();
//    if (entrySetSize == 0) { return Collections.EMPTY_LIST; }
//
//    Object[] tmp = new Object[entrySetSize];
//    int index = -1;
//    for (Iterator i = fullEntrySet.iterator(); i.hasNext();) {
//      Map.Entry e = (Map.Entry)i.next();
//      if (!(e.getValue() instanceof ObjectID)) {
//        index++;
//        tmp[index] = e;
//      }
//    }
//
//    if (index < 0) { return Collections.EMPTY_LIST; }
//    Object[] rv = new Object[index + 1];
//    System.arraycopy(tmp, 0, rv, 0, index + 1);
//    return Arrays.asList(rv);
//  }
}