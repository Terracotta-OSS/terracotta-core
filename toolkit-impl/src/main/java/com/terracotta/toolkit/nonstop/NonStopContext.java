/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.AsyncToolkitInitializer;

public interface NonStopContext {

  NonStopManager getNonStopManager();

  NonStopConfigRegistryImpl getNonStopConfigurationRegistry();

  AbortableOperationManager getAbortableOperationManager();

  NonstopTimeoutBehaviorResolver getNonstopTimeoutBehaviorResolver();

  AsyncToolkitInitializer getAsyncToolkitInitializer();

  NonStopClusterListener getNonStopClusterListener();
}
