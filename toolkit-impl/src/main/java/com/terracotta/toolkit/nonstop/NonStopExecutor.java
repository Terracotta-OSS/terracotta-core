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

import net.sf.ehcache.util.NamedThreadFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NonStopExecutor {
  public final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
      new NamedThreadFactory("NonStopExecutor", true));

  public Future schedule(Runnable task, long timeout) {
    return executor.schedule(task, timeout, TimeUnit.MILLISECONDS);
  }

  public void remove(Future future) {
    if (future instanceof Runnable) {
      executor.remove((Runnable) future);
    }
  }

  public void shutdown() {
    executor.shutdown();
  }

}
