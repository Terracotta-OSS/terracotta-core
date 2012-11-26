/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.abortable.AbortableOperationManager;

import java.util.concurrent.FutureTask;

public class NonstopToolkitLockDelegateProvider extends AbstractNonStopDelegateProvider<ToolkitLock> {

  private final String        name;
  private final ToolkitLockTypeInternal lockType;

  public NonstopToolkitLockDelegateProvider(AbortableOperationManager abortableOperationManager,
                                             NonStopConfigRegistryImpl nonStopConfigRegistry,
                                             NonstopTimeoutBehaviorResolver behaviorResolver,
                                            FutureTask<ToolkitInternal> toolkitDelegateFutureTask, String name,
                                            ToolkitLockTypeInternal lockType) {
    super(toolkitDelegateFutureTask, abortableOperationManager, nonStopConfigRegistry, behaviorResolver, name);
    this.name = name;
    this.lockType = lockType;
  }

  @Override
  public ToolkitLock getToolkitObject() {
    return getToolkit().getLock(name, lockType);
  }

  @Override
  public ToolkitObjectType getTolkitObjectType() {
    return ToolkitObjectType.READ_WRITE_LOCK;
  }

}
