/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.feature;

import org.terracotta.toolkit.ToolkitFeature;

public abstract class EnabledToolkitFeature implements ToolkitFeature {

  @Override
  public final boolean isEnabled() {
    return true;
  }

}
