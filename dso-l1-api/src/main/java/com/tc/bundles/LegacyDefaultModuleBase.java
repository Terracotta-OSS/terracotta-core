/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import com.tc.object.config.LockDefinition;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public abstract class LegacyDefaultModuleBase {

  protected final StandardDSOClientConfigHelper configHelper;

  public LegacyDefaultModuleBase(StandardDSOClientConfigHelper configHelper) {
    this.configHelper = configHelper;
  }

  public abstract void apply();

  protected TransparencyClassSpec getOrCreateSpec(String expr, boolean markAsPreInstrumented) {
    TransparencyClassSpec spec = configHelper.getOrCreateSpec(expr);
    if (markAsPreInstrumented) spec.markPreInstrumented();
    return spec;
  }

  protected TransparencyClassSpec getOrCreateSpec(String expr) {
    return getOrCreateSpec(expr, true);
  }

  protected void addLock(String expr, LockDefinition ld) {
    configHelper.addLock(expr, ld);
  }

}
