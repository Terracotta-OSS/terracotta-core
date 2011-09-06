/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
