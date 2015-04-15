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

import com.tc.object.locks.ThreadID;
import com.tc.util.VicariousThreadLocal;

public class ThreadIDManagerImpl implements ThreadIDManager {

  private final ThreadLocal threadID;
  private long              threadIDSequence;
  private final ThreadIDMap threadIDMap;

  public ThreadIDManagerImpl(final ThreadIDMap threadIDMap) {
    this.threadID = new VicariousThreadLocal();
    this.threadIDMap = threadIDMap;
  }

  @Override
  public ThreadID getThreadID() {
    ThreadID rv = (ThreadID) threadID.get();
    if (rv == null) {
      rv = new ThreadID(nextThreadID(), Thread.currentThread().getName());
      threadIDMap.addTCThreadID(rv);
      threadID.set(rv);
    }
    return rv;
  }

  private synchronized long nextThreadID() {
    return ++threadIDSequence;
  }
}
