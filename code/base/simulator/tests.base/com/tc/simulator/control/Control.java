/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.control;

import com.tc.util.TCTimeoutException;

public interface Control {
  void waitForStart(long timeout) throws TCTimeoutException, TCBrokenBarrierException, InterruptedException;

  void notifyComplete();

  void notifyMutationComplete();

  void notifyValidationStart();

  /**
   * Returns true if all participants completed in time.
   */
  boolean waitForAllComplete(long timeout) throws InterruptedException;

  /**
   * Returns true if all participants completed in time.
   */
  boolean waitForMutationComplete(long timeout) throws InterruptedException;

  /**
   * Returns true if all participants completed in time.
   */
  boolean waitForValidationStart(long timeout) throws InterruptedException;
}
