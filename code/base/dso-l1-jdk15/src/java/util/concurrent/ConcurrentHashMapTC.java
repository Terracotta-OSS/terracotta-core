/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package java.util.concurrent;

import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.TCMap;

import java.util.Collection;

public abstract class ConcurrentHashMapTC extends ConcurrentHashMap implements TCMap, Manageable {
  public Collection __tc_getAllEntriesSnapshot() {
    throw new UnsupportedOperationException();
  }

  public Collection __tc_getAllLocalEntriesSnapshot() {
    throw new UnsupportedOperationException();
  }
}