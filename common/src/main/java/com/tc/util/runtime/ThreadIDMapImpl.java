/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.runtime;

import com.tc.object.locks.ThreadID;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadIDMapImpl implements ThreadIDMap {
  private final ReferenceQueue<ThreadID> referenceQueue = new ReferenceQueue<ThreadID>();
  private final Map<Long, WeakReference<ThreadID>> id2ThreadIDMap = new ConcurrentHashMap<Long, WeakReference<ThreadID>>();
  private Thread collectorThread;

  @Override
  public synchronized void addTCThreadID(ThreadID tcThreadID) {
    cleanupReferenceQueue();
    id2ThreadIDMap.put(Long.valueOf(Thread.currentThread().getId()), new WeakReference<ThreadID>(tcThreadID, referenceQueue));
  }



  @Override
  public synchronized ThreadID getTCThreadID(Long javaThreadId) {
    cleanupReferenceQueue();
    return id2ThreadIDMap.get(javaThreadId).get();
  }

  private void cleanupReferenceQueue() {
    Reference<? extends ThreadID> ref;
    while ((ref = referenceQueue.poll()) != null) {
      for (Map.Entry<Long, WeakReference<ThreadID>> entry : id2ThreadIDMap.entrySet()) {
        if (entry.getValue().equals(ref)) {
          id2ThreadIDMap.remove(entry.getKey());
        }
      }
    }
  }
  /** For testing only - not in interface */
  public synchronized int getSize() {
    return id2ThreadIDMap.size();
  }

}
