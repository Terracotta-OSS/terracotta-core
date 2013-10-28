/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.feature.NonStopInternalFeature;

import com.terracotta.toolkit.feature.EnabledToolkitFeature;
import com.terracotta.toolkit.nonstop.NonStopContextImpl;

public class NonStopInternalFeatureImpl extends EnabledToolkitFeature implements NonStopInternalFeature {
  private final NonStopContextImpl nonStopContext;

  public NonStopInternalFeatureImpl(NonStopContextImpl nonStopContext) {
    this.nonStopContext = nonStopContext;
  }

  @Override
  public void enableForCurrentThread(boolean enable) {
    nonStopContext.enableForCurrentThread(enable);
  }

}
