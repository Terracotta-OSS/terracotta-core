/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.AsyncToolkitInitializer;

// TODO: make a builder
public class NonStopContextImpl implements NonStopContext {

  private final NonStopManager                 nonStopManager;

  private final NonStopConfigRegistryImpl      nonStopConfigRegistryImpl;

  private final AbortableOperationManager      abortableOperationManager;

  private final NonstopTimeoutBehaviorResolver nonstopTimeoutBehaviorResolver;

  private final AsyncToolkitInitializer        asyncToolkitInitializer;

  private final NonStopClusterListener         nonStopClusterListener;

  public NonStopContextImpl(NonStopManager nonStopManager, NonStopConfigRegistryImpl nonStopConfigRegistryImpl,
                            AbortableOperationManager abortableOperationManager,
                            NonstopTimeoutBehaviorResolver nonstopTimeoutBehaviorResolver,
                            AsyncToolkitInitializer asyncToolkitInitializer,
                            NonStopClusterListener nonStopClusterListener) {
    this.nonStopManager = nonStopManager;
    this.nonStopConfigRegistryImpl = nonStopConfigRegistryImpl;
    this.abortableOperationManager = abortableOperationManager;
    this.nonstopTimeoutBehaviorResolver = nonstopTimeoutBehaviorResolver;
    this.asyncToolkitInitializer = asyncToolkitInitializer;
    this.nonStopClusterListener = nonStopClusterListener;
  }

  @Override
  public NonStopManager getNonStopManager() {
    return nonStopManager;
  }

  @Override
  public NonStopConfigRegistryImpl getNonStopConfigurationRegistry() {
    return nonStopConfigRegistryImpl;
  }

  @Override
  public AbortableOperationManager getAbortableOperationManager() {
    return abortableOperationManager;
  }

  @Override
  public NonstopTimeoutBehaviorResolver getNonstopTimeoutBehaviorResolver() {
    return nonstopTimeoutBehaviorResolver;
  }

  @Override
  public AsyncToolkitInitializer getAsyncToolkitInitializer() {
    return asyncToolkitInitializer;
  }

  @Override
  public NonStopClusterListener getNonStopClusterListener() {
    return nonStopClusterListener;
  }

}
