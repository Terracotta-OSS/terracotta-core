/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcsimulator;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CountDown;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CountDownSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.control.Control;
import com.tc.simulator.control.TCBrokenBarrierException;
import com.tc.simulator.listener.MutationCompletionListener;

public class ControlImpl implements Control, MutationCompletionListener {
  private static final boolean DEBUG = false;

  private final int            mutatorCount;
  private final int            completeParties;
  private final CyclicBarrier  startBarrier;
  private final CountDown      validationStartCount;
  private final CountDown      countdown;
  private final int            validatorCount;
  private final Control        testWideControl;
  private final boolean        crashActiveServerAfterMutate;

  private CountDown            mutationCompleteCount;
  private long                 executionTimeout;

  public ControlImpl(int mutatorCount) {
    this(mutatorCount, 0, false);
  }

  // used to create container-wide control
  public ControlImpl(int mutatorCount, Control testWideControl) {
    this(mutatorCount, 0, testWideControl, false);
  }

  // used to create test-wide control
  public ControlImpl(int mutatorCount, int validatorCount, boolean crashActiveServerAfterMutate) {
    this(mutatorCount, validatorCount, null, crashActiveServerAfterMutate);
  }

  public ControlImpl(int mutatorCount, int validatorCount, Control testWideControl, boolean crashActiveServerAfterMutate) {
    if (mutatorCount < 0 || validatorCount < 0) { throw new AssertionError(
                                                                           "MutatorCount and validatorCount must be non-negative numbers!  mutatorCount=["
                                                                               + mutatorCount + "] validatorCount=["
                                                                               + validatorCount + "]"); }

    executionTimeout = -1;
    this.testWideControl = testWideControl;
    this.crashActiveServerAfterMutate = crashActiveServerAfterMutate;
    this.mutatorCount = mutatorCount;
    this.validatorCount = validatorCount;
    completeParties = mutatorCount + validatorCount;

    debugPrintln("####### completeParties=[" + completeParties + "]");

    startBarrier = new CyclicBarrier(this.mutatorCount);
    mutationCompleteCount = new CountDown(this.mutatorCount);
    countdown = new CountDown(completeParties);

    // "1" indicates the server
    if (this.crashActiveServerAfterMutate) {
      validationStartCount = new CountDown(completeParties + 1);
    } else {
      validationStartCount = new CountDown(completeParties);
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String classname = ControlImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");

    String cyclicBarrierClassname = CyclicBarrier.class.getName();
    config.addIncludePattern(cyclicBarrierClassname);
    config.addWriteAutolock("* " + cyclicBarrierClassname + ".*(..)");

    String countdownClassname = CountDown.class.getName();
    config.addIncludePattern(countdownClassname);
    config.addWriteAutolock("* " + countdownClassname + ".*(..)");
  }

  public static void visitDSOApplicationConfig(com.tc.object.config.ConfigVisitor visitor,
                                               com.tc.object.config.DSOApplicationConfig config) {
    String classname = ControlImpl.class.getName();
    config.addIncludePattern(classname);
    config.addWriteAutolock("* " + classname + ".*(..)");

    new CyclicBarrierSpec().visit(visitor, config);
    new CountDownSpec().visit(visitor, config);

  }

  public String toString() {
    return getClass().getName() + "[ mutatorCount=" + mutatorCount + ", completeParties=" + completeParties
           + ", startBarrier=" + startBarrier + ", countdown=" + countdown + ", mutationCompleteCount="
           + mutationCompleteCount + ", validatorCount=" + validatorCount + " ]" + ", crashActiveServerAfterMutate=["
           + crashActiveServerAfterMutate + "]";
  }

  public void waitForStart() throws InterruptedException, TCBrokenBarrierException{
    try {
      try {
        this.startBarrier.barrier();
      } catch (InterruptedException e1) {
        throw e1;
      }
    } catch (BrokenBarrierException e) {
      throw new TCBrokenBarrierException(e);
    }
  }

  public void notifyMutationComplete() {
    mutationCompleteCount.release();
  }

  public void notifyValidationStart() {
    debugPrintln("********  validation.release() called:  init=[" + validationStartCount.initialCount() + "] before=["
                 + validationStartCount.currentCount() + "]");
    validationStartCount.release();
    debugPrintln("******* validation.release() called:  after=[" + validationStartCount.currentCount() + "]");
  }

  /*
   * Control interface method
   */
  public boolean waitForMutationComplete(long timeout) throws InterruptedException {
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      checkExecutionTimeout(timeout);
      boolean rv = mutationCompleteCount.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  public boolean waitForValidationStart(long timeout) throws InterruptedException {
    debugPrintln("*******  waitForValidationStart:  validationStartCount=[" + validationStartCount.currentCount() + "]");
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      checkExecutionTimeout(timeout);
      boolean rv = validationStartCount.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  /*
   * MutationCompletionListener interface method -- called by applications
   */
  public void waitForMutationCompleteTestWide() throws Exception {
    try {
      if (testWideControl == null) { throw new AssertionError(
                                                              "Application should be calling this on container-wide control, not test-wide control."); }
      checkExecutionTimeout(executionTimeout);
      boolean rv = testWideControl.waitForMutationComplete(executionTimeout);
      if (!rv) { throw new RuntimeException("Wait on MutationCompletionCount did not pass:  executionTimeout=["
                                            + executionTimeout + "] "); }
    } catch (InterruptedException e) {
      throw e;
    }
  }

  public void waitForValidationStartTestWide() throws Exception {
    try {
      if (testWideControl == null) { throw new AssertionError(
                                                              "Application should be calling this on container-wide control, not test-wide control."); }
      checkExecutionTimeout(executionTimeout);
      boolean rv = testWideControl.waitForValidationStart(executionTimeout);
      if (!rv) { throw new RuntimeException("Wait on ValidationStartCount did not pass:  executionTimeout=["
                                            + executionTimeout + "] "); }
    } catch (InterruptedException e) {
      throw e;
    }
  }

  public void notifyComplete() {
    debugPrintln("*******  countdown called:  control=[" + toString() + "]");
    this.countdown.release();
  }

  public boolean waitForAllComplete(long timeout) throws InterruptedException {
    if (timeout < 0) {
      while (true) {
        synchronized (this) {
          wait();
        }
      }
    }
    try {
      checkExecutionTimeout(timeout);
      boolean rv = this.countdown.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }

  public void setExecutionTimeout(long timeout) {
    checkExecutionTimeout(timeout);
    executionTimeout = timeout;
  }

  private void checkExecutionTimeout(long timeout) {
    if (timeout < 0) { throw new AssertionError("Execution timeout should be a non-negative number:  timeout=["
                                                + timeout + "]"); }
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }
}