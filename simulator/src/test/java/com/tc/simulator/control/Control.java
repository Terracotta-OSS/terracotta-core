/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.control;

public interface Control {
  void waitForStart() throws TCBrokenBarrierException, InterruptedException;

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
