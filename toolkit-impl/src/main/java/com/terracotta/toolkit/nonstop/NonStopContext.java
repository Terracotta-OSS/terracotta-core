/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.AsyncToolkitInitializer;

public interface NonStopContext {

  NonStopManager getNonStopManager();

  NonStopConfigurationRegistry getNonStopConfigurationRegistry();

  AbortableOperationManager getAbortableOperationManager();

  NonstopTimeoutBehaviorResolver getNonstopTimeoutBehaviorResolver();

  AsyncToolkitInitializer getAsyncToolkitInitializer();

  NonStopClusterListener getNonStopClusterListener();

  boolean isEnabledForCurrentThread();

}
