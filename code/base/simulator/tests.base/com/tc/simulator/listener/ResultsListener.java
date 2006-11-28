/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.listener;

import com.tc.simulator.app.ErrorContext;

public interface ResultsListener {
  
  /**
   * Timed out waiting for all parties to join.
   */
  public void notifyStartTimeout();
  /**
   * Timed out waiting for execution to complete.
   */
  public void notifyExecutionTimeout();
  /**
   * Encountered an error/exception during execution.
   */
  public void notifyError(ErrorContext ectxt);
  /**
   * Execution completed with the given result.
   */
  public void notifyResult(Object result);

}
