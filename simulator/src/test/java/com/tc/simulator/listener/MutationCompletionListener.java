/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.simulator.listener;

public interface MutationCompletionListener {

  void notifyMutationComplete();
  
  void notifyValidationStart();

  void waitForMutationCompleteTestWide() throws Exception;

  void waitForValidationStartTestWide() throws Exception;
}
