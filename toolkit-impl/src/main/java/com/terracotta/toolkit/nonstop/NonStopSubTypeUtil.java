/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

public class NonStopSubTypeUtil {

  private static Set<Class> SUPPORTED_SUB_TYPES = new HashSet<Class>();

  static {
    SUPPORTED_SUB_TYPES.add(Iterator.class);
    SUPPORTED_SUB_TYPES.add(ListIterator.class);
    SUPPORTED_SUB_TYPES.add(Collection.class);
    SUPPORTED_SUB_TYPES.add(Set.class);
    SUPPORTED_SUB_TYPES.add(List.class);
    SUPPORTED_SUB_TYPES.add(Map.class);
    SUPPORTED_SUB_TYPES.add(SortedMap.class);
    SUPPORTED_SUB_TYPES.add(SortedSet.class);
    SUPPORTED_SUB_TYPES.add(ToolkitLock.class);
    SUPPORTED_SUB_TYPES.add(ToolkitReadWriteLock.class);
  }

  public static boolean isNonStopSubtype(Class klazz) {
    return SUPPORTED_SUB_TYPES.contains(klazz);
  }
}
