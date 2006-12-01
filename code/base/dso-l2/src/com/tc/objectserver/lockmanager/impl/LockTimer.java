/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.lockmanager.api.LockAwardContext;
import com.tc.objectserver.lockmanager.api.LockEventListener;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of a lock timer.
 */
public class LockTimer implements LockEventListener {

  private static final TCLogger   logger      = TCLogging.getLogger(LockTimer.class);

  private final Timer             timer       = new Timer(true);
  private final DSOChannelManager channelManager;
  private final Map               tasks       = new HashMap();
  private final Set               uncontended = new HashSet();

  LockTimer(DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  synchronized void startTimerForLock(LockAwardContext awardContext) {
    logStartTimerForLock(awardContext);
    LockTimeoutTask task = new LockTimeoutTask(awardContext);
    putTimerTask(task);
    timer.schedule(task, task.getTimeout());
  }

  synchronized LockAwardContext cancel(LockAwardContext awardContext) {
    logCancel(awardContext);
    LockTimeoutTask task = removeTimerTask(awardContext);
    if (task != null) {
      task.cancel();
      return task.awardContext;
    } else {
      return null;
    }
  }

  /*********************************************************************************************************************
   * LockEventListener interface
   */

  /**
   * If the waiter count is 1, then this is the first waiter. We should schedule a timeout for this lock award context.
   * Otherwise, we shouldn't do anything.
   */
  public synchronized void notifyAddPending(int waiterCount, LockAwardContext ctxt) {
    Assert.eval(waiterCount > 0);
    if (waiterCount == 1) {
      logNotifyAddWaitingStartTimer(waiterCount, ctxt);
      startTimerForLock(ctxt);
    } else {
      logNotifyAddWaitingUncontended(waiterCount, ctxt);
      uncontended.add(ctxt);
    }
  }

  private void logNotifyAddWaitingStartTimer(int waiterCount, LockAwardContext ctxt) {
    if (logger.isDebugEnabled()) {
      logger.debug("notifyAddWaiting(waiterCount=" + waiterCount + ", ctxt=" + ctxt + "): starting lock timer...");
    }
  }

  private void logNotifyAddWaitingUncontended(int waiterCount, LockAwardContext ctxt) {
    if (logger.isDebugEnabled()) {
      logger.debug("notifyAddWaiting(watierCount=" + waiterCount + ", ctxt=" + ctxt + "): adding to uncontended...");
    }
  }

  /**
   * If the waiter count is greater than zero, we should schedule a timeout for this lock award context. Otherwise, we
   * shouldn't do anything.
   */
  public synchronized void notifyAward(int waiterCount, LockAwardContext ctxt) {
    Assert.eval(waiterCount >= 0);
    if (waiterCount > 0) {
      logNotifyAwardStartTimer(waiterCount, ctxt);
      startTimerForLock(ctxt);
    } else {
      logNotifyAwardUncontended(waiterCount, ctxt);
      uncontended.add(ctxt);
    }
  }

  private void logNotifyAwardStartTimer(int waiterCount, LockAwardContext ctxt) {
    if (logger.isDebugEnabled()) {
      logger.debug("notifyAward(waiterCount=" + waiterCount + ", ctxt=" + ctxt + "): starting lock timer...");
    }
  }

  private void logNotifyAwardUncontended(int waiterCount, LockAwardContext ctxt) {
    if (logger.isDebugEnabled()) {
      logger.debug("notifyAward(waiterCount=" + waiterCount + ", ctxt=" + ctxt + "): adding to uncontended...");
    }
  }

  /**
   * We should cancel any existing timers for the given lock award context.
   */
  public synchronized void notifyRevoke(LockAwardContext ctxt) {
    LockAwardContext cancelled = cancel(ctxt);
    logNotifyRevoke(ctxt, cancelled);
    if (cancelled == null) {
      // check to make sure that it was uncontended
      Assert.eval("Attempt to revoke a lock that was not awarded and not uncontended.", uncontended.remove(ctxt));
    }
  }

  private void logNotifyRevoke(LockAwardContext ctxt, LockAwardContext cancelled) {
    if (logger.isDebugEnabled()) {
      logger.debug("notifyRevoke(ctxt=" + ctxt + "): cancelled=" + cancelled);
    }
  }

  /*********************************************************************************************************************
   * Private stuff.
   */

  private void putTimerTask(LockTimeoutTask task) {
    synchronized (this.tasks) {
      this.tasks.put(task.awardContext, task);
    }
  }

  private LockTimeoutTask removeTimerTask(LockAwardContext awardContext) {
    synchronized (this.tasks) {
      return (LockTimeoutTask) this.tasks.remove(awardContext);
    }
  }

  private void logStartTimerForLock(LockAwardContext awardContext) {
    if (logger.isDebugEnabled()) {
      logger.debug("startTimerForLock(" + awardContext + ")");
    }
  }

  private void logCancel(LockAwardContext awardContext) {
    if (logger.isDebugEnabled()) {
      logger.debug("cancel(" + awardContext + ")");
    }
  }

  private class LockTimeoutTask extends TimerTask {
    private final LockAwardContext awardContext;

    private LockTimeoutTask(LockAwardContext ctxt) {
      Assert.assertNotNull(ctxt);
      Assert.assertNotNull(ctxt.getChannelID());
      this.awardContext = ctxt;
    }

    long getTimeout() {
      return awardContext.getTimeout();
    }

    public void run() {
      logger.warn("Lock timeout: " + this.awardContext);
      try {
        MessageChannel channel = channelManager.getChannel(this.awardContext.getChannelID());
        logger.warn("Closing channel because of lock timeout.  Award context: " + this.awardContext + "; channel: "
                    + channel);
        channel.close();
      } catch (NoSuchChannelException e) {
        logger.warn("Attempting to close channel because of lock timeout.  Couldn't find channel by channel id: "
                    + this.awardContext.getChannelID());
      }
    }
  }

}