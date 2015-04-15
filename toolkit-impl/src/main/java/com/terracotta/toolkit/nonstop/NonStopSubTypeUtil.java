/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
