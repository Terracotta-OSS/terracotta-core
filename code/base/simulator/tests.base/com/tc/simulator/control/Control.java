/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.control;

import com.tc.util.TCTimeoutException;

public interface Control {
  public void waitForStart(long timeout) throws TCTimeoutException, TCBrokenBarrierException, InterruptedException;
  
  public void notifyComplete();
  
  /**
   * Returns true if all participants completed in time.
   */
  public boolean waitForAllComplete(long timeout) throws InterruptedException;
}
