/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.ToolkitInitializer;

// TODO: make a builder
public class NonStopContextImpl implements NonStopContext {

  private final NonStopManager                 nonStopManager;

  private final NonStopConfigRegistryImpl      nonStopConfigRegistryImpl;

  private final AbortableOperationManager      abortableOperationManager;

  private final NonstopTimeoutBehaviorResolver nonstopTimeoutBehaviorResolver;

  private final ToolkitInitializer             toolkitInitializer;

  private final NonStopClusterListener         nonStopClusterListener;

  private final ThreadLocal<Boolean>           enabledForCurrentThread = new ThreadLocal<Boolean>();

  public NonStopContextImpl(NonStopManager nonStopManager, NonStopConfigRegistryImpl nonStopConfigRegistryImpl,
                            AbortableOperationManager abortableOperationManager,
                            NonstopTimeoutBehaviorResolver nonstopTimeoutBehaviorResolver,
                            ToolkitInitializer toolkitInitializer,
                            NonStopClusterListener nonStopClusterListener) {
    this.nonStopManager = nonStopManager;
    this.nonStopConfigRegistryImpl = nonStopConfigRegistryImpl;
    this.abortableOperationManager = abortableOperationManager;
    this.nonstopTimeoutBehaviorResolver = nonstopTimeoutBehaviorResolver;
    this.toolkitInitializer = toolkitInitializer;
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
  public ToolkitInitializer getToolkitInitializer() {
    return toolkitInitializer;
  }

  @Override
  public NonStopClusterListener getNonStopClusterListener() {
    return nonStopClusterListener;
  }

  @Override
  public boolean isEnabledForCurrentThread() {
    return enabledForCurrentThread.get() == null;
  }

  public void enableForCurrentThread(boolean enable) {
    if (enable) {
      enabledForCurrentThread.remove();
    } else {
      enabledForCurrentThread.set(Boolean.FALSE);
    }
  }

}
