/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.lockmanager.api.TCLockTimer;
import com.tc.object.lockmanager.api.TimerCallback;
import com.tc.object.tx.TimerSpec;
import com.tc.object.tx.TimerSpec.Signature;
import com.tc.util.Assert;
import com.tc.util.TCTimerImpl;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages timed lock waits
 * 
 * @author teck
 */
public class TCLockTimerImpl implements TCLockTimer {
  private static final TCLogger logger   = TCLogging.getLogger(TCLockTimer.class);

  private final Timer           timer    = new TCTimerImpl("DSO Lock Object.wait() timer", true);
  private boolean               shutdown = false;

  public TCLockTimerImpl() {
    super();
  }

  public Timer getTimer() {
    return timer;
  }

  public TimerTask scheduleTimer(TimerCallback callback, TimerSpec call, Object callbackObject) {
    final Signature signature = call.getSignature();

    if (signature == TimerSpec.NO_ARGS) {
      return null;
    } else if (signature == TimerSpec.LONG) {
      if (call.getMillis() == 0) { return null; }
    } else if (signature == TimerSpec.LONG_INT) {
      if ((call.getMillis() == 0) && (call.getNanos() == 0)) { return null; }
    } else {
      throw Assert.failure("unknown wait signature: " + signature);
    }

    final TimerTask rv = new TaskImpl(callback, call, callbackObject);

    if (signature == TimerSpec.LONG_INT) {
      // logger.warn("Nanosecond granualarity not yet supported, adding 1ms to wait time instead");
      timer.schedule(rv, call.getMillis() + 1);
    } else {
      timer.schedule(rv, call.getMillis());
    }
    call.mark();

    return rv;
  }

  private static class TaskImpl extends TimerTask {

    private final TimerCallback callback;
    private final Object            callbackObject;
    private final TimerSpec    call;

    TaskImpl(TimerCallback callback, TimerSpec call, Object callbackObject) {
      this.callback = callback;
      this.call = call;
      this.callbackObject = callbackObject;
    }

    public void run() {
      try {
        callback.timerTimeout(callbackObject);
      } catch (Exception e) {
        logger.error("Error processing wait timeout for " + callbackObject, e);
      }
    }

    public boolean cancel() {
      call.adjust();
      return super.cancel();
    }
  }

  public synchronized void shutdown() {
    if (shutdown) return;
    shutdown = true;
    this.timer.cancel();
  }

}
