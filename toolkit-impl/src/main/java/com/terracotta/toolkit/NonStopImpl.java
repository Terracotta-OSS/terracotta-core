/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;

import com.terracotta.toolkit.feature.EnabledToolkitFeature;

public class NonStopImpl extends EnabledToolkitFeature implements NonStopFeature {
  private final NonStopToolkitImpl nonStopToolkitImpl;

  public NonStopImpl(NonStopToolkitImpl nonStopToolkitImpl) {
    this.nonStopToolkitImpl = nonStopToolkitImpl;
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

}
