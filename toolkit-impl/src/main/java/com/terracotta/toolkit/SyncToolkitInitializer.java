/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.ToolkitInternal;

public class SyncToolkitInitializer implements ToolkitInitializer {

  private final ToolkitInternal toolkit;

  public SyncToolkitInitializer(ToolkitInternal toolkit) {
    this.toolkit = toolkit;
  }

  @Override
  public ToolkitInternal getToolkit() {
    return toolkit;
  }

  @Override
  public ToolkitInternal getToolkitOrNull() {
    return toolkit;
  }

}
