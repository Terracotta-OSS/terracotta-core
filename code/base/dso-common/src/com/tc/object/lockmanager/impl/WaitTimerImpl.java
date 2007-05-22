/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.lockmanager.api.WaitTimer;
import com.tc.object.lockmanager.api.WaitTimerCallback;
import com.tc.object.tx.WaitInvocation;
import com.tc.object.tx.WaitInvocation.Signature;
import com.tc.util.Assert;
import com.tc.util.TCTimerImpl;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages timed lock waits
 * 
 * @author teck
 */
public class WaitTimerImpl implements WaitTimer {
  private static final TCLogger logger   = TCLogging.getLogger(WaitTimer.class);

  private final Timer           timer    = new TCTimerImpl("DSO Lock Object.wait() timer", true);
  private boolean               shutdown = false;
  //public WaitInvocation         originalCall;

  public WaitTimerImpl() {
    super();
  }

  public Timer getTimer() {
    return timer;
  }

  public TimerTask scheduleTimer(WaitTimerCallback callback, WaitInvocation call, Object callbackObject) {
    //this.originalCall = call;
    final Signature signature = call.getSignature();

    if (signature == WaitInvocation.NO_ARGS) {
      return null;
    } else if (signature == WaitInvocation.LONG) {
      if (call.getMillis() == 0) { return null; }
    } else if (signature == WaitInvocation.LONG_INT) {
      if ((call.getMillis() == 0) && (call.getNanos() == 0)) { return null; }
    } else {
      throw Assert.failure("unknown wait signature: " + signature);
    }

    final TimerTask rv = new TaskImpl(callback, call, callbackObject);

    if (signature == WaitInvocation.LONG_INT) {
      // logger.warn("Nanosecond granualarity not yet supported, adding 1ms to wait time instead");
      timer.schedule(rv, call.getMillis() + 1);
    } else {
      timer.schedule(rv, call.getMillis());
    }
    call.mark();

    return rv;
  }

  private static class TaskImpl extends TimerTask {

    private final WaitTimerCallback callback;
    private final Object            callbackObject;
    private final WaitInvocation    call;

    TaskImpl(WaitTimerCallback callback, WaitInvocation call, Object callbackObject) {
      this.callback = callback;
      this.call = call;
      this.callbackObject = callbackObject;
    }

    public void run() {
      try {
        callback.waitTimeout(callbackObject);
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
