/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.terracotta.toolkit.ToolkitInitializer;

public class ToolkitLockLookup implements ToolkitObjectLookup<ToolkitLock> {
  private final ToolkitLockTypeInternal lockType;
  private final String                  name;
  private final ToolkitInitializer      toolkitInitializer;

  public ToolkitLockLookup(ToolkitInitializer toolkitInitializer, String name,
                           ToolkitLockTypeInternal lockType) {
    this.toolkitInitializer = toolkitInitializer;
    this.name = name;
    this.lockType = lockType;
  }

  @Override
  public ToolkitLock getInitializedObject() {
    return toolkitInitializer.getToolkit().getLock(name, lockType);
  }

  @Override
  public ToolkitLock getInitializedObjectOrNull() {
    ToolkitInternal toolkitInternal = toolkitInitializer.getToolkitOrNull();
    if (toolkitInternal != null) {
      return toolkitInternal.getLock(name, lockType);
    } else {
      return null;
    }
  }
}