/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit;

import org.terracotta.toolkit.internal.ToolkitInternal;

public interface ToolkitInitializer {

  public ToolkitInternal getToolkit();

  public ToolkitInternal getToolkitOrNull();
}
