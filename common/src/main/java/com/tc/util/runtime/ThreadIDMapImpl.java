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
package com.tc.util.runtime;

import com.google.common.collect.MapMaker;
import com.tc.object.locks.ThreadID;

import java.util.Map;

public class ThreadIDMapImpl implements ThreadIDMap {
  private final Map<Long, ThreadID> id2ThreadIDMap = new MapMaker().weakValues().makeMap();

  @Override
  public synchronized void addTCThreadID(final ThreadID tcThreadID) {
    id2ThreadIDMap.put(Long.valueOf(Thread.currentThread().getId()), tcThreadID);
  }

  @Override
  public synchronized ThreadID getTCThreadID(final Long javaThreadId) {
    return id2ThreadIDMap.get(javaThreadId);
  }

  /** For testing only - not in interface */
  public synchronized int getSize() {
    return id2ThreadIDMap.size();
  }

}
