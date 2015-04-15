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
package com.terracotta.toolkit.util;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.store.ToolkitStore;

public class ToolkitIDGeneratorImpl implements ToolkitIDGenerator {
  private final ToolkitLock  lock;
  private final String       name;
  private final ToolkitStore map;

  public ToolkitIDGeneratorImpl(String name, ToolkitStore map) {
    this.name = name;
    this.map = map;
    this.lock = map.createLockForKey(name).writeLock();
    // do a get first time to init the value
    getFromMap();
  }

  @Override
  public long getId() {
    return getFromMap();
  }

  private Long getFromMap() {
    Long rv = (Long) map.get(name);
    if (rv != null) {
      return rv;
    } else {
      lock.lock();
      try {
        // Need to manually do a put if absent here so that we avoid calling putIfAbsent unconditionally. The problem
        // is that putIfAbsent will trigger throttling (possibly throwing an exception) if the server is full. This
        // would
        // prevent toolkit from bootstrapping, thereby preventing the user from spinning up a new toolkit client in
        // order
        // to recover from the server full situation.
        rv = (Long) map.get(name);
        if (rv == null) {
          rv = 1L;
          map.put(name, rv);
        }
        return rv;
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  public void incrementId() {
    lock.lock();
    try {
      Long result = getFromMap() + 1;
      map.putNoReturn(name, result);
    } finally {
      lock.unlock();
    }
  }

}
