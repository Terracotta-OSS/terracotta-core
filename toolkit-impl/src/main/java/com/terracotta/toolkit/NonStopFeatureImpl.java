/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;

import com.tc.abortable.AbortableOperationManager;
import com.terracotta.toolkit.feature.EnabledToolkitFeature;

public class NonStopFeatureImpl extends EnabledToolkitFeature implements NonStopFeature {
  private final NonStopToolkitImpl nonStopToolkitImpl;
  private final AbortableOperationManager abortableOperationManager;

  public NonStopFeatureImpl(NonStopToolkitImpl nonStopToolkitImpl, AbortableOperationManager abortableOperationManager) {
    this.nonStopToolkitImpl = nonStopToolkitImpl;
    this.abortableOperationManager = abortableOperationManager;
  }

  @Override
  public void start(NonStopConfiguration nonStopConfig) {
    if (nonStopConfig == null) { return; }

    nonStopToolkitImpl.start(nonStopConfig);
  }

  @Override
  public void finish() {
    nonStopToolkitImpl.stop();
  }

  @Override
  public NonStopConfigurationRegistry getNonStopConfigurationRegistry() {
    return nonStopToolkitImpl.getNonStopConfigurationToolkitRegistry();
  }

  @Override
  public boolean isTimedOut() {
    return abortableOperationManager.isAborted();
  }

}
