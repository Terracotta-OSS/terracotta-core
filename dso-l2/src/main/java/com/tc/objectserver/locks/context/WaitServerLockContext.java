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
package com.tc.objectserver.locks.context;

import com.tc.net.ClientID;
import com.tc.object.locks.ThreadID;

import java.util.TimerTask;

public class WaitServerLockContext extends LinkedServerLockContext {
  private TimerTask  task;
  private final long timeout;

  public WaitServerLockContext(ClientID clientID, ThreadID threadID, long timeout) {
    this(clientID, threadID, timeout, null);
  }

  public WaitServerLockContext(ClientID clientID, ThreadID threadID, long timeout, TimerTask task) {
    super(clientID, threadID);
    this.timeout = timeout;
    this.task = task;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimerTask(TimerTask task) {
    this.task = task;
  }

  public TimerTask getTimerTask() {
    return task;
  }
}
