/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;
import EDU.oswego.cs.dl.util.concurrent.CountDown;
import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.spec.CountDownSpec;
import com.tc.object.config.spec.CyclicBarrierSpec;
import com.tc.simulator.control.Control;
import com.tc.simulator.control.TCBrokenBarrierException;
import com.tc.util.TCTimeoutException;

public class ControlImpl implements Control {
  private final int startParties;
  private final int completeParties;
  private final CyclicBarrier startBarrier;
  private final CountDown countdown;

  public ControlImpl(int parties) {
    this(parties, parties);
  }
  
  public ControlImpl(int startParties, int completeParties) {
    this.startParties = startParties;
    this.completeParties = completeParties;
    this.startBarrier = new CyclicBarrier(startParties);
    this.countdown = new CountDown(completeParties);
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
    return getClass().getName() + "[ startParties=" + startParties + ", completeParties=" +
    completeParties + ", startBarrier="
    + startBarrier + ", countdown=" + countdown + "]";
  }
  
  public void waitForStart(long timeout) throws InterruptedException, TCBrokenBarrierException, TCTimeoutException {
    try {
      try {
        this.startBarrier.attemptBarrier(timeout);
      } catch (InterruptedException e1) {
        throw e1;
      }
    } catch (TimeoutException e) {
      throw new TCTimeoutException(e);
    } catch (BrokenBarrierException e) {
      throw new TCBrokenBarrierException(e);
    }
  }

  public void notifyComplete() {
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
      boolean rv = this.countdown.attempt(timeout);
      return rv;
    } catch (InterruptedException e) {
      throw e;
    }
  }
}