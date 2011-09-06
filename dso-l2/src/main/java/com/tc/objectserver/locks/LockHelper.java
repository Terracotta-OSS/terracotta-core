/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.locks;

import com.tc.async.api.Sink;
import com.tc.management.L2LockStatsManager;
import com.tc.object.locks.ServerLockContextStateMachine;
import com.tc.objectserver.locks.timer.LockTimer;
import com.tc.objectserver.locks.timer.TimerCallback;

public class LockHelper {
  private final LockTimer                     lockTimer;
  private final Sink                          lockSink;
  private final LockStore                     lockStore;
  private final ServerLockContextStateMachine contextStateMachine;
  private L2LockStatsManager                  lockStatsManager;
  private final TimerCallback                 timerCallback;

  public LockHelper(L2LockStatsManager lockStatsManager, Sink lockSink, LockStore lockStore, TimerCallback timerCallback) {
    this.lockTimer = new LockTimer();
    this.lockStatsManager = lockStatsManager;
    this.lockSink = lockSink;
    this.lockStore = lockStore;
    this.timerCallback = timerCallback;
    this.contextStateMachine = new ServerLockContextStateMachine();
  }

  public LockTimer getLockTimer() {
    return lockTimer;
  }

  public L2LockStatsManager getLockStatsManager() {
    return lockStatsManager;
  }

  public void setLockStatsManager(L2LockStatsManager lockStatsManager) {
    this.lockStatsManager = lockStatsManager;
  }

  public Sink getLockSink() {
    return lockSink;
  }

  public LockStore getLockStore() {
    return lockStore;
  }

  public ServerLockContextStateMachine getContextStateMachine() {
    return contextStateMachine;
  }

  public TimerCallback getTimerCallback() {
    return timerCallback;
  }
}
